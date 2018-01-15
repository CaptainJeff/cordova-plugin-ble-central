// (c) 2014-2016 Don Coleman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.megster.cordova.ble.central;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Build;

import android.provider.Settings;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.*;

public class BLECentralPlugin extends CordovaPlugin implements BluetoothAdapter.LeScanCallback {
    // actions
    private static final String SCAN = "scan";
    private static final String START_SCAN = "startScan";
    private static final String STOP_SCAN = "stopScan";
    private static final String START_SCAN_WITH_OPTIONS = "startScanWithOptions";

    private static final String LIST = "list";

    private static final String CONNECT = "connect";
    private static final String DISCONNECT = "disconnect";

    private static final String READ = "read";
    private static final String WRITE = "write";
    private static final String WRITE_WITHOUT_RESPONSE = "writeWithoutResponse";

    private static final String READ_RSSI = "readRSSI";

    private static final String START_NOTIFICATION = "startNotification"; // register for characteristic notification
    private static final String STOP_NOTIFICATION = "stopNotification"; // remove characteristic notification

    private static final String IS_ENABLED = "isEnabled";
    private static final String IS_CONNECTED  = "isConnected";

    private static final String SETTINGS = "showBluetoothSettings";
    private static final String ENABLE = "enable";

    private static final String START_STATE_NOTIFICATIONS = "startStateNotifications";
    private static final String STOP_STATE_NOTIFICATIONS = "stopStateNotifications";

    public String MAC_ADDRESS;


    // callbacks
    CallbackContext discoverCallback;
    private CallbackContext enableBluetoothCallback;

    private static final String TAG = "BLECoachCare";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;

    BluetoothAdapter bluetoothAdapter;

    // key is the MAC Address
    Map<String, Peripheral> peripherals = new LinkedHashMap<String, Peripheral>();

    // scan options
    boolean reportDuplicates = false;

    // Android 23 requires new permissions for BluetoothLeScanner.startScan()
    private static final String ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 2;
    private static final int PERMISSION_DENIED_ERROR = 20;
    private CallbackContext permissionCallback;
    private UUID[] serviceUUIDs;
    private int scanSeconds;
    private MAC_ADDRESS string;
    

    // Bluetooth state notification
    CallbackContext stateCallback;
    BroadcastReceiver stateReceiver;
    Map<Integer, String> bluetoothStates = new Hashtable<Integer, String>() {{
        put(BluetoothAdapter.STATE_OFF, "off");
        put(BluetoothAdapter.STATE_TURNING_OFF, "turningOff");
        put(BluetoothAdapter.STATE_ON, "on");
        put(BluetoothAdapter.STATE_TURNING_ON, "turningOn");
    }};

    public void onDestroy() {
        removeStateListener();
    }

    public void onReset() {
        removeStateListener();
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        LOG.d(TAG, "action = " + action);


        // String macAddress = args.getString(0);
        UUID serviceUUID = uuidFromString(Helper.CommandCode.trackerServiceUuid);
        UUID characteristicUUID = uuidFromString(Helper.CommandCode.trackerCharacteristicWriteUuid);

        if (bluetoothAdapter == null) {
            Activity activity = cordova.getActivity();
            boolean hardwareSupportsBLE = activity.getApplicationContext()
                                            .getPackageManager()
                                            .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
                                            Build.VERSION.SDK_INT >= 18;
            if (!hardwareSupportsBLE) {
              LOG.w(TAG, "This hardware does not support Bluetooth Low Energy.");
              callbackContext.error("This hardware does not support Bluetooth Low Energy.");
              return false;
            }
            BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        boolean validAction = true;

        if (action.equals(SCAN)) {

            UUID[] serviceUUIDs = parseServiceUUIDList(args.getJSONArray(0));
            int scanSeconds = args.getInt(1);
            resetScanOptions();
            findLowEnergyDevices(callbackContext, serviceUUIDs, scanSeconds);

        } else if (action.equals(START_SCAN)) {

            UUID[] serviceUUIDs = parseServiceUUIDList(args.getJSONArray(0));
            resetScanOptions();
            findLowEnergyDevices(callbackContext, serviceUUIDs, -1);

        } else if (action.equals(STOP_SCAN)) {

            bluetoothAdapter.stopLeScan(this);
            callbackContext.success();

        } else if (action.equals(LIST)) {

            listKnownDevices(callbackContext);

        } else if (action.equals(CONNECT)) {
            MAC_ADDRESS = args.getString(0);
            connect(callbackContext, MAC_ADDRESS);

        } else if (action.equals(DISCONNECT)) {

            disconnect(callbackContext, MAC_ADDRESS);

        } else if (action.equals(READ)) {

            characteristicUUID = uuidFromString(args.getString(2));
            read(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID);

        } else if (action.equals(READ_RSSI)) {

            readRSSI(callbackContext, MAC_ADDRESS);

        } else if (action.equals(WRITE)) {

            byte[] data = args.getArrayBuffer(3);
            int type = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
            write(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID, data, type);

        } else if (action.equals(WRITE_WITHOUT_RESPONSE)) {

            byte[] data = args.getArrayBuffer(3);
            int type = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
            write(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID, data, type);

        } else if (action.equals(START_NOTIFICATION)) {
            characteristicUUID = uuidFromString(Helper.CommandCode.trackerCharacteristicReadUuid);
            registerNotifyCallback(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID);

        } else if (action.equals(STOP_NOTIFICATION)) {

            characteristicUUID = uuidFromString(args.getString(2));
            removeNotifyCallback(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID);

        } else if (action.equals(IS_ENABLED)) {

            if (bluetoothAdapter.isEnabled()) {
                callbackContext.success();
            } else {
                callbackContext.error("Bluetooth is disabled.");
            }

        } else if (action.equals(IS_CONNECTED)) {

            if (peripherals.containsKey(MAC_ADDRESS) && peripherals.get(MAC_ADDRESS).isConnected()) {
                callbackContext.success();
            } else {
                callbackContext.error("Not connected.");
            }

        } else if (action.equals(SETTINGS)) {

            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            cordova.getActivity().startActivity(intent);
            callbackContext.success();

        } else if (action.equals(ENABLE)) {

            enableBluetoothCallback = callbackContext;
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            cordova.startActivityForResult(this, intent, REQUEST_ENABLE_BLUETOOTH);

        } else if (action.equals(START_STATE_NOTIFICATIONS)) {

            if (this.stateCallback != null) {
                callbackContext.error("State callback already registered.");
            } else {
                this.stateCallback = callbackContext;
                addStateListener();
                sendBluetoothStateChange(bluetoothAdapter.getState());
            }

        } else if (action.equals(STOP_STATE_NOTIFICATIONS)) {

            if (this.stateCallback != null) {
                // Clear callback in JavaScript without actually calling it
                PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                result.setKeepCallback(false);
                this.stateCallback.sendPluginResult(result);
                this.stateCallback = null;
            }
            removeStateListener();
            callbackContext.success();

        } else if (action.equals(START_SCAN_WITH_OPTIONS)) {
            UUID[] serviceUUIDs = parseServiceUUIDList(args.getJSONArray(0));
            JSONObject options = args.getJSONObject(1);

            resetScanOptions();
            this.reportDuplicates = options.optBoolean("reportDuplicates", false);
            findLowEnergyDevices(callbackContext, serviceUUIDs, -1);

        } else if (action.equals("activateVibration")) {
            int duration = args.getInt(0);

            byte[] data = new byte[16];
            data[0] = Helper.CommandCode.activateVibration;;
            data[1] = (byte) (duration > 10 ? 10 : duration);
            data[15] = Helper.calcCRC(data);
            
            int type = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
            write(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID, data, type);
        } else if (action.equals("setTimeFormat")) {
            String timeFormat = args.getString(1);

            byte[] data = new byte[16];
            data[0] = Helper.CommandCode.setTimeFormat;
            data[1] = (byte) (timeFormat == "12" ? 0x00 : 0x01);
            data[15] = Helper.calcCRC(data);
            
            int type = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
            write(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID, data, type);
        } else if (action.equals("setMode")) {
            String mode = args.getString(1);

            final byte[] data = new byte[16];
            data[0] = Helper.CommandCode.setMode;
            data[1] = (mode.equalsIgnoreCase("sleep") ? (byte) 0x01 : (byte) 0x02);
            data[15] = Helper.calcCRC(data);
            
            int type = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
            write(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID, data, type);
        } else if (action.equals("setDeviceTime")) {
            String timeStamp = args.getString(1);
    
            final byte[] data = new byte[16];
            data[0] = Helper.CommandCode.setDeviceTime;
            data[1] = Byte.parseByte(timeStamp.substring(2,4), 16);
            data[2] = Byte.parseByte(timeStamp.substring(5,7), 16);
            data[3] = Byte.parseByte(timeStamp.substring(8,10), 16);
            data[4] = Byte.parseByte(timeStamp.substring(11,13), 16);
            data[5] = Byte.parseByte(timeStamp.substring(14,16), 16);
            data[6] = Byte.parseByte(timeStamp.substring(17,18), 16);
            data[15] = Helper.calcCRC(data);
            
            int type = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
            write(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID, data, type);
        } else if (action.equals("getSoftwareVersion")) {
            byte[] data = new byte[16];
            data[0] = Helper.CommandCode.getSoftwareVersion;
            data[15] = Helper.calcCRC(data);
            
            int type = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
            write(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID, data, type);
        } else if (action.equals("getDetailedDayActivity")) {

            byte[] data = new byte[16];
            data[0] = Helper.CommandCode.getDetailedCurrentDayActivityData;
            data[1] = (byte) 1;
            data[15] = Helper.calcCRC(data);
            
            int type = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
            
            write(callbackContext, MAC_ADDRESS, serviceUUID, characteristicUUID, data, type);
        } 
        
        // else if (action.equalsIgnoreCase("getSummaryDaySleep")) {
        //     getSleepData = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     JSONObject info = args.getJSONObject(0);
        //     String date = info.getString("date");
        //     String deviceDate = info.getString("deviceDate");


        //     tracker.getSleepSummary(deviceDate != null ? deviceDate : new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        //   } else if (action.equalsIgnoreCase("getLastSleepActivity")) {
        //     getSleepData = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.getSleepData();
        //   } else if (action.equalsIgnoreCase("disconnect")) {
        //     diconnectCallBack = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     String id = args.getString(0);
        //     tracker.disconnect(id);
        //     scale.disconnect(id);
        //   } else if (action.equalsIgnoreCase("startSession")) {
        //     startSessionCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.startSession();
        //   } else if (action.equalsIgnoreCase("scanAll")) {

        //   } else if (action.equalsIgnoreCase("setDeviceTime")) {
        //     timeCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.setDateTime(args.getString(0));
        //   } else if (action.equalsIgnoreCase("getDeviceTime")) {
        //     timeCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.getCurrentTime();
        //   } else if (action.equalsIgnoreCase("setUserPersonalInfo")) {
        //     userInfoCallback = callbackContext;
        //     JSONObject request = args.getJSONObject(0);
        //     tracker.setUserPersonalInfo(request.getString("gender"),
        //       request.getInt("age"),
        //       request.getInt("height"),
        //       request.getInt("weight"),
        //       request.getInt("strideLength"));
        //   } else if (action.equalsIgnoreCase("getUserPersonalInfo")) {
        //     userInfoCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.getUsetPersonalInfo();
        //   } else if (action.equalsIgnoreCase("getDetailedDayActivity")) {
        //     dailyActivityCallback = callbackContext;
        //     tracker.getDayActivity(args.getJSONObject(0).getString("date"));
        //   } else if (action.equalsIgnoreCase("getSummaryDayActivity")) {
        //     summaryCallback = callbackContext;
        //     JSONObject info = args.getJSONObject(0);
        //     String date = info.getString("date");
        //     String deviceDate = info.getString("deviceDate");
        //     tracker.getDaySummary(deviceDate != null ? deviceDate : new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        //   } else if (action.equalsIgnoreCase("getLastActivity")) {
        //     latestActivityCallback = callbackContext;
        //     tracker.getLatestActivity();
        //   } else if (action.equalsIgnoreCase("setTargetSteps")) {
        //     tracker.setTargetSteps(args.getInt(0));
        //   } else if (action.equalsIgnoreCase("getTargetSteps")) {
        //     targetStepsCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.getTargetSteps();
        //   } else if (action.equalsIgnoreCase("setDistanceUnit")) {
        //     distanceUnitCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.setDistanceUnit(args.getString(0));
        //   } else if (action.equalsIgnoreCase("getDistanceUnit")) {
        //     distanceUnitCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.getDistanceUnit();
        //   } else if (action.equalsIgnoreCase("getDevicesBatteryStatus")) {
        //     batteryCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.getBatteryStatus();
        //   } else if (action.equalsIgnoreCase("getSoftwareVersion")) {
        //     versionCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.getSoftVersion();
        //   } else if (action.equalsIgnoreCase("activateVibration")) {
        //     vibrateCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.vibrate(args.getInt(0));
        //   } else if (action.equalsIgnoreCase("setTimeFormat")) {
        //     tracker.setTimeFormat(args.getString(0));
        //   } else if (action.equalsIgnoreCase("getTimeFormat")) {
        //     tracker.getTimeFormat();
        //   } else if (action.equalsIgnoreCase("setDeviceName")) {
        //     tracker.setDeviceName(args.getString(0));
        //   } else if (action.equalsIgnoreCase("getDeviceName")) {
        //     tracker.getDeviceName();
        //   } else if (action.equalsIgnoreCase("showMessage")) {
        //   } else if (action.equalsIgnoreCase("setMode")) {
        //     modeCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     final String mode = args.getString(0);
        //     tracker.switchTOActivityOrSleepMode(mode);
        //   } else if (action.equalsIgnoreCase("getMode")) {
        //     modeCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     tracker.getActivityOrSleepMode();
        //   } else if (action.equalsIgnoreCase("getDataFromScales")) {
        //     scaleCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //     scaleResponse = false;
        //     JSONObject userProfile = args.getJSONObject(1);
        //     gender = userProfile.getString("gender");
        //     age = userProfile.getInt("age");
        //     height = userProfile.getInt("height");
        //     weight = userProfile.getDouble("weight");
        //     strideLength = userProfile.getDouble("strideLength");
        //     scale.syncScales("");
        //   } else if (action.equalsIgnoreCase("stopScan")) {
        //     tracker.stopScan();
        //     scale.stopScan();
        //     callbackContext.success();
        //   } else if (action.equalsIgnoreCase("StartLogging")) {
        //     logCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //   } else if (action.equalsIgnoreCase("getLastTime")) {
        //     lastSleepTimeCallback = new CallbackContext(callbackContext.getCallbackId(), cWebView);
        //   }
        
        else {

            validAction = false;

        }

        return validAction;
    }

    private UUID[] parseServiceUUIDList(JSONArray jsonArray) throws JSONException {
        List<UUID> serviceUUIDs = new ArrayList<UUID>();

        for(int i = 0; i < jsonArray.length(); i++){
            String uuidString = jsonArray.getString(i);
            serviceUUIDs.add(uuidFromString(uuidString));
        }

        return serviceUUIDs.toArray(new UUID[jsonArray.length()]);
    }

    private void onBluetoothStateChange(Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            sendBluetoothStateChange(state);
        }
    }

    private void sendBluetoothStateChange(int state) {
        if (this.stateCallback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, this.bluetoothStates.get(state));
            result.setKeepCallback(true);
            this.stateCallback.sendPluginResult(result);
        }
    }

    private void addStateListener() {
        if (this.stateReceiver == null) {
            this.stateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    onBluetoothStateChange(intent);
                }
            };
        }

        try {
            IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            webView.getContext().registerReceiver(this.stateReceiver, intentFilter);
        } catch (Exception e) {
            LOG.e(TAG, "Error registering state receiver: " + e.getMessage(), e);
        }
    }

    private void removeStateListener() {
        if (this.stateReceiver != null) {
            try {
                webView.getContext().unregisterReceiver(this.stateReceiver);
            } catch (Exception e) {
                LOG.e(TAG, "Error unregistering state receiver: " + e.getMessage(), e);
            }
        }
        this.stateCallback = null;
        this.stateReceiver = null;
    }

    private void connect(CallbackContext callbackContext, String macAddress) {
        Peripheral peripheral = peripherals.get(macAddress);
        // MAC_ADDRESS = macAddress;
        if (peripheral != null) {
            peripheral.connect(callbackContext, cordova.getActivity());
        } else {
            callbackContext.error("Peripheral " + macAddress + " not found.");
        }

    }

    private void disconnect(CallbackContext callbackContext, String macAddress) {

        Peripheral peripheral = peripherals.get(macAddress);
        if (peripheral != null) {
            peripheral.disconnect();
        }
        callbackContext.success();

    }

    private void read(CallbackContext callbackContext, String macAddress, UUID serviceUUID, UUID characteristicUUID) {

        Peripheral peripheral = peripherals.get(macAddress);

        if (peripheral == null) {
            callbackContext.error("Peripheral " + macAddress + " not found.");
            return;
        }

        if (!peripheral.isConnected()) {
            callbackContext.error("Peripheral " + macAddress + " is not connected.");
            return;
        }

        //peripheral.readCharacteristic(callbackContext, serviceUUID, characteristicUUID);
        peripheral.queueRead(callbackContext, serviceUUID, characteristicUUID);

    }

    private void readRSSI(CallbackContext callbackContext, String macAddress) {

        Peripheral peripheral = peripherals.get(macAddress);

        if (peripheral == null) {
            callbackContext.error("Peripheral " + macAddress + " not found.");
            return;
        }

        if (!peripheral.isConnected()) {
            callbackContext.error("Peripheral " + macAddress + " is not connected.");
            return;
        }
        peripheral.queueReadRSSI(callbackContext);
    }

    private void write(CallbackContext callbackContext, String macAddress, UUID serviceUUID, UUID characteristicUUID,
                       byte[] data, int writeType) {

        Peripheral peripheral = peripherals.get(macAddress);

        if (peripheral == null) {
            callbackContext.error("Peripheral " + macAddress + " not found.");
            return;
        }

        if (!peripheral.isConnected()) {
            callbackContext.error("Peripheral " + macAddress + " is not connected.");
            return;
        }
        LOG.d(TAG, "macAddress " + macAddress);
        LOG.d(TAG, "MAC_ADDRESS " + MAC_ADDRESS);
        //peripheral.writeCharacteristic(callbackContext, serviceUUID, characteristicUUID, data, writeType);
        peripheral.queueWrite(callbackContext, serviceUUID, characteristicUUID, data, writeType);

    }

    private void registerNotifyCallback(CallbackContext callbackContext, String macAddress, UUID serviceUUID, UUID characteristicUUID) {

        Peripheral peripheral = peripherals.get(macAddress);
        if (peripheral != null) {

            if (!peripheral.isConnected()) {
                callbackContext.error("Peripheral " + macAddress + " is not connected.");
                return;
            }

            //peripheral.setOnDataCallback(serviceUUID, characteristicUUID, callbackContext);
            peripheral.queueRegisterNotifyCallback(callbackContext, serviceUUID, characteristicUUID);

        } else {

            callbackContext.error("Peripheral " + macAddress + " not found");

        }

    }

    private void removeNotifyCallback(CallbackContext callbackContext, String macAddress, UUID serviceUUID, UUID characteristicUUID) {

        Peripheral peripheral = peripherals.get(macAddress);
        if (peripheral != null) {

            if (!peripheral.isConnected()) {
                callbackContext.error("Peripheral " + macAddress + " is not connected.");
                return;
            }

            peripheral.queueRemoveNotifyCallback(callbackContext, serviceUUID, characteristicUUID);

        } else {

            callbackContext.error("Peripheral " + macAddress + " not found");

        }

    }

    private void findLowEnergyDevices(CallbackContext callbackContext, UUID[] serviceUUIDs, int scanSeconds) {
        LOG.d(TAG, "findLowEnergyDevices ");
        if(!PermissionHelper.hasPermission(this, ACCESS_COARSE_LOCATION)) {
            // save info so we can call this method again after permissions are granted
            LOG.d(TAG, "no permission ");
            permissionCallback = callbackContext;
            this.serviceUUIDs = serviceUUIDs;
            this.scanSeconds = scanSeconds;
            PermissionHelper.requestPermission(this, REQUEST_ACCESS_COARSE_LOCATION, ACCESS_COARSE_LOCATION);
            return;
        } else {
            LOG.d(TAG, "has permission ");
        }

        // ignore if currently scanning, alternately could return an error
        if (bluetoothAdapter.isDiscovering()) {
            return;
        }

        // clear non-connected cached peripherals
        for(Iterator<Map.Entry<String, Peripheral>> iterator = peripherals.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Peripheral> entry = iterator.next();
            Peripheral device = entry.getValue();
            boolean connecting = device.isConnecting();
            if (connecting){
                LOG.d(TAG, "Not removing connecting device: " + device.getDevice().getAddress());
            }
            if(!entry.getValue().isConnected() && !connecting) {
                iterator.remove();
            }
        }

        discoverCallback = callbackContext;

        if (serviceUUIDs.length > 0) {
            bluetoothAdapter.startLeScan(serviceUUIDs, this);
        } else {
            bluetoothAdapter.startLeScan(this);
        }

        if (scanSeconds > 0) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    LOG.d(TAG, "Stopping Scan");
                    BLECentralPlugin.this.bluetoothAdapter.stopLeScan(BLECentralPlugin.this);
                }
            }, scanSeconds * 1000);
        }

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    private void listKnownDevices(CallbackContext callbackContext) {

        JSONArray json = new JSONArray();

        // do we care about consistent order? will peripherals.values() be in order?
        for (Map.Entry<String, Peripheral> entry : peripherals.entrySet()) {
            Peripheral peripheral = entry.getValue();
            json.put(peripheral.asJSONObject());
        }

        PluginResult result = new PluginResult(PluginResult.Status.OK, json);
        callbackContext.sendPluginResult(result);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

        String address = device.getAddress();
        boolean alreadyReported = peripherals.containsKey(address);

        if (!alreadyReported) {

            Peripheral peripheral = new Peripheral(device, rssi, scanRecord);
            peripherals.put(device.getAddress(), peripheral);

            if (discoverCallback != null) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, peripheral.asJSONObject());
                result.setKeepCallback(true);
                discoverCallback.sendPluginResult(result);
            }

        } else {
            Peripheral peripheral = peripherals.get(address);
            peripheral.update(rssi, scanRecord);
            if (reportDuplicates && discoverCallback != null) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, peripheral.asJSONObject());
                result.setKeepCallback(true);
                discoverCallback.sendPluginResult(result);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {

            if (resultCode == Activity.RESULT_OK) {
                LOG.d(TAG, "User enabled Bluetooth");
                if (enableBluetoothCallback != null) {
                    enableBluetoothCallback.success();
                }
            } else {
                LOG.d(TAG, "User did *NOT* enable Bluetooth");
                if (enableBluetoothCallback != null) {
                    enableBluetoothCallback.error("User did not enable Bluetooth");
                }
            }

            enableBluetoothCallback = null;
        }
    }

    /* @Override */
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) /* throws JSONException */ {
        for(int result:grantResults) {
            if(result == PackageManager.PERMISSION_DENIED)
            {
                LOG.d(TAG, "User *rejected* Coarse Location Access");
                this.permissionCallback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                return;
            }
        }

        switch(requestCode) {
            case REQUEST_ACCESS_COARSE_LOCATION:
                LOG.d(TAG, "User granted Coarse Location Access");
                findLowEnergyDevices(permissionCallback, serviceUUIDs, scanSeconds);
                this.permissionCallback = null;
                this.serviceUUIDs = null;
                this.scanSeconds = -1;
                break;
        }
    }

    private UUID uuidFromString(String uuid) {
        return UUIDHelper.uuidFromString(uuid);
    }

    /**
     * Reset the BLE scanning options
     */
    private void resetScanOptions() {
        this.reportDuplicates = false;
    }

    // private Byte commandLists(String data) {
    //     return Helper.CommandCode.data;
    // }

}