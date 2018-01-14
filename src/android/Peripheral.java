// (c) 2104 Don Coleman
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

import android.app.Activity;

import android.bluetooth.*;
import android.os.Build;
import android.util.Base64;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Peripheral wraps the BluetoothDevice and provides methods to convert to JSON.
 */
public class Peripheral extends BluetoothGattCallback {

    // 0x2902 org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
    //public final static UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    public final static UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUIDHelper.uuidFromString("2902");
    private static final String TAG = "Peripheral";

    private BluetoothDevice device;
    private byte[] advertisingData;
    private int advertisingRSSI;
    private boolean connected = false;
    private boolean connecting = false;
    private ConcurrentLinkedQueue<BLECommand> commandQueue = new ConcurrentLinkedQueue<BLECommand>();
    private boolean bleProcessing;

    ArrayList<ActivityData> dayActivity;
    ArrayList<SleepData> daySleep;

    BluetoothGatt gatt;

    private CallbackContext connectCallback;
    private CallbackContext readCallback;
    private CallbackContext writeCallback;

    private Map<String, CallbackContext> notificationCallbacks = new HashMap<String, CallbackContext>();

    public Peripheral(BluetoothDevice device, int advertisingRSSI, byte[] scanRecord) {

        this.device = device;
        this.advertisingRSSI = advertisingRSSI;
        this.advertisingData = scanRecord;

    }

    public void connect(CallbackContext callbackContext, Activity activity) {
        BluetoothDevice device = getDevice();
        connecting = true;

        connectCallback = callbackContext;
        if (Build.VERSION.SDK_INT < 23) {
            gatt = device.connectGatt(activity, false, this);
        } else {
            gatt = device.connectGatt(activity, false, this, BluetoothDevice.TRANSPORT_LE);
        }

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    public void disconnect() {
        connectCallback = null;
        connected = false;
        connecting = false;

        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
    }

    public JSONObject asJSONObject()  {

        JSONObject json = new JSONObject();

        try {
            json.put("name", device.getName());
            json.put("id", device.getAddress()); // mac address
            json.put("advertising", byteArrayToJSON(advertisingData));
            // TODO real RSSI if we have it, else
            json.put("rssi", advertisingRSSI);
        } catch (JSONException e) { // this shouldn't happen
            e.printStackTrace();
        }

        return json;
    }

    public JSONObject asJSONObject(String errorMessage)  {

        JSONObject json = new JSONObject();

        try {
            json.put("name", device.getName());
            json.put("id", device.getAddress()); // mac address
            json.put("errorMessage", errorMessage);
        } catch (JSONException e) { // this shouldn't happen
            e.printStackTrace();
        }

        return json;
    }

    public JSONObject asJSONObject(BluetoothGatt gatt) {

        JSONObject json = asJSONObject();

        try {
            JSONArray servicesArray = new JSONArray();
            JSONArray characteristicsArray = new JSONArray();
            json.put("services", servicesArray);
            json.put("characteristics", characteristicsArray);

            if (connected && gatt != null) {
                for (BluetoothGattService service : gatt.getServices()) {
                    servicesArray.put(UUIDHelper.uuidToString(service.getUuid()));

                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        JSONObject characteristicsJSON = new JSONObject();
                        characteristicsArray.put(characteristicsJSON);

                        characteristicsJSON.put("service", UUIDHelper.uuidToString(service.getUuid()));
                        characteristicsJSON.put("characteristic", UUIDHelper.uuidToString(characteristic.getUuid()));
                        //characteristicsJSON.put("instanceId", characteristic.getInstanceId());

                        characteristicsJSON.put("properties", Helper.decodeProperties(characteristic));
                            // characteristicsJSON.put("propertiesValue", characteristic.getProperties());

                        if (characteristic.getPermissions() > 0) {
                            characteristicsJSON.put("permissions", Helper.decodePermissions(characteristic));
                            // characteristicsJSON.put("permissionsValue", characteristic.getPermissions());
                        }

                        JSONArray descriptorsArray = new JSONArray();

                        for (BluetoothGattDescriptor descriptor: characteristic.getDescriptors()) {
                            JSONObject descriptorJSON = new JSONObject();
                            descriptorJSON.put("uuid", UUIDHelper.uuidToString(descriptor.getUuid()));
                            descriptorJSON.put("value", descriptor.getValue()); // always blank

                            if (descriptor.getPermissions() > 0) {
                                descriptorJSON.put("permissions", Helper.decodePermissions(descriptor));
                                // descriptorJSON.put("permissionsValue", descriptor.getPermissions());
                            }
                            descriptorsArray.put(descriptorJSON);
                        }
                        if (descriptorsArray.length() > 0) {
                            characteristicsJSON.put("descriptors", descriptorsArray);
                        }
                    }
                }
            }
        } catch (JSONException e) { // TODO better error handling
            e.printStackTrace();
        }

        return json;
    }

    static JSONObject byteArrayToJSON(byte[] bytes) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("CDVType", "ArrayBuffer");
        object.put("data", Base64.encodeToString(bytes, Base64.NO_WRAP));
        return object;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isConnecting() {
        return connecting;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, this.asJSONObject(gatt));
            result.setKeepCallback(true);
            connectCallback.sendPluginResult(result);
        } else {
            LOG.e(TAG, "Service discovery failed. status = " + status);
            connectCallback.error(this.asJSONObject("Service discovery failed"));
            disconnect();
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        this.gatt = gatt;

        if (newState == BluetoothGatt.STATE_CONNECTED) {

            connected = true;
            connecting = false;
            gatt.discoverServices();

        } else {

            if (connectCallback != null) {
                connectCallback.error(this.asJSONObject("Peripheral Disconnected"));
            }
            disconnect();
        }

    }

    // @Override
    public void parseResponse(byte[] value) {
      LOG.d(TAG, "response~~ " + value);
      LOG.d(TAG, "response~~ " + value[0]);
      LOG.d(TAG, "Helper.CommandCode.getDetailedCurrentDayActivityData~~ " + Helper.CommandCode.getDetailedCurrentDayActivityData);
      LOG.d(TAG, "Helper.CommandCode.getDetailedCurrentDayActivityData~~ " + Helper.CommandCode.getDetailedCurrentDayActivityDataResponse);
      LOG.d(TAG, "Helper.CommandCode.getDetailedCurrentDayActivityData~~ " + Helper.CommandCode.getDetailedCurrentDayActivityDataResponseByte);
      
      if (value[0] == Helper.CommandCode.getSoftwareVersion) {
        getSoftVersionResponse(value);
      } else if (value[0] == Helper.CommandCode.getDevicesBatteryStatus) {

        writeCallback.success();
        commandCompleted();
      } else if (value[0] == Helper.CommandCode.getTargetSteps) {

        writeCallback.success();
        commandCompleted();
      } else if (value[0] == Helper.CommandCode.getDeviceName) {

        writeCallback.success();
        commandCompleted();
      } else if (value[0] == Helper.CommandCode.getTimeFormat) {

        writeCallback.success();
        commandCompleted();
      } else if (value[0] == Helper.CommandCode.getDeviceTime) {

        writeCallback.success();
        commandCompleted();
      } else if (value[0] == Helper.CommandCode.getUserPersonalInfo) {

        writeCallback.success();
        commandCompleted();
      } else if (value[0] == Helper.CommandCode.getDetailedCurrentDayActivityData) {
        LOG.d(TAG, "dayActivityResponse " + value[0]);
        dayActivityResponse(value);
      } else if (value[0] == Helper.CommandCode.getDistanceUnit) {

        writeCallback.success();
        commandCompleted();
      } else if (value[0] == Helper.CommandCode.getMode) {

        writeCallback.success();
        commandCompleted();
      } else if (value[0] == Helper.CommandCode.activateVibrationResponse || value[0] == Helper.CommandCode.activateVibration ) {
        LOG.d(TAG, "value!!! " + value[0]);
        // response = onSuccessCall();
        writeCallback.success();
        commandCompleted();
      }
       else {
        LOG.d(TAG, "elllllse!!! " + value[0]);
        writeCallback.success();
        commandCompleted();
      }
      
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        LOG.d(TAG, "onCharacteristicChanged " + characteristic);

        CallbackContext callback = notificationCallbacks.get(generateHashKey(characteristic));

        if (callback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, characteristic.getValue());
            result.setKeepCallback(true);
            callback.sendPluginResult(result);
            // writeCallback.sendPluginResult(result);
            // writeCallback.success();
        }
        
        parseResponse(characteristic.getValue());
    }

    private void dayActivityResponse(byte[] response) {
      LOG.d(TAG, "dayActivityResponse " + response);
      LOG.d(TAG, "dayActivityResponse - 0 " + response[0]);
      LOG.d(TAG, "dayActivityResponse - 6 " + response[6]);
      LOG.d(TAG, "dayActivityResponse - 1 " + response[1]);
      ArrayList<SleepData> lastSleepData;
      if (response[6] != (byte) (0xff)) {
        LOG.d(TAG, "if " + response[6]);
        dayActivity.add(new ActivityData(response));
        LOG.d(TAG, "dayActivity " + String.valueOf(dayActivity));
      } else {
        LOG.d(TAG, "else " + response[6]);
        daySleep.add(new SleepData(response));
      }
      if (response[1] != (byte) 0xff) {
        LOG.d(TAG, "321 " + String.valueOf(dayActivity));
        LOG.d(TAG, "322 " + String.valueOf(dayActivity.size()));
        if (dayActivity.size() + daySleep.size() == 96) {
          LOG.d(TAG, "324 " + String.valueOf(dayActivity.size()));
        //   LOG.d(TAG, "Error", String.valueOf(dayActivity.size() + daySleep.size()));
        //   switch (state) {
        //     case SUMMARY: {
        //       int totalSteps = 0;
        //       float totalCal = 0;
        //       float totalDistanse = 0;
        //       for (int i = 0; i < dayActivity.size(); i++) {
        //         totalSteps += dayActivity.get(i).getSteps();
        //         totalCal += dayActivity.get(i).calories;
        //         totalDistanse += dayActivity.get(i).distance;
        //       }
        //       trackerAPICallback.onSummaryResponse(true, summaryDay, totalSteps, totalCal, totalDistanse);
        //       break;
        //     }
        //     case DAY:
        //       trackerAPICallback.onDailyActivityResponse(true, dayActivity.toArray(new ActivityData[dayActivity.size()]));
        //       break;
        //     case LATEST: {
        //       int totalSteps = 0;
        //       float totalCal = 0;
        //       float totalDistanse = 0;
        //       String date = "";
        //       for (int i = 0; i < dayActivity.size(); i++) {
        //         totalSteps += dayActivity.get(i).getSteps();
        //         totalCal += dayActivity.get(i).calories;
        //         totalDistanse += dayActivity.get(i).distance;
        //         if (dayActivity.get(i).getSteps() != 0) {
        //           date = dayActivity.get(i).getTime();
        //         }
        //       }
        //       trackerAPICallback.onLatestActivityResponse(true, date, totalSteps, totalCal, totalDistanse);
        //       break;
        //     }
        //     case SLEEP: {
        //       trackerAPICallback.onLastSleepResponse(true, daySleep.toArray(new SleepData[daySleep.size()]));
        //       lastSleepData = daySleep;
        //       //calcSleepTime();
        //       break;
        //     }
        //     case SUMMARY_SLEEP: {
        //       lastSleepData = daySleep;
        //       //calcSleepTime();
        //       trackerAPICallback.onSummarySleepResponse(true, getSleepFrames(daySleep.toArray(new SleepData[daySleep.size()])));
        //       break;
        //     }
        //     case SLEEP_TIME: {
        //       LOG.d(TAG,"SLEEP_TIME", "");
        //       lastSleepData = daySleep;
        //       calcSleepTime();
        //       break;
        //     }
        //     case SLEEP_YESTERDAY_TIME: {
        //       lastSleepData = daySleep;
        //       calcSleepTime();
        //       break;
        //     }
        //   }
        } else {
          LOG.d(TAG,"Error", String.valueOf(dayActivity.size() + daySleep.size()));
          LOG.d(TAG, "384 " + String.valueOf(dayActivity.size()));
        }
      } 
      else {
        LOG.d(TAG, "388 " + response[0]);
        LOG.d(TAG, "389 " + String.valueOf(dayActivity.size()));
        // switch (state) {
        //   case SUMMARY:
        //     trackerAPICallback.onSummaryResponse(true, summaryDay, 0, 0, 0);
        //     break;
        //   case DAY:
        //     trackerAPICallback.onDailyActivityResponse(false, null);
        //     break;
        //   case LATEST:
        //     trackerAPICallback.onDailyActivityResponse(false, null);
        //     break;
        //   case SLEEP:
        //     trackerAPICallback.onLastSleepResponse(false, null);
        //     break;
        //   case SUMMARY_SLEEP:
        //     trackerAPICallback.onSummarySleepResponse(false, null);
        //     break;
        //   case SLEEP_TIME:
        //     trackerAPICallback.onSleepTime(false, 0, "");
        //     break;
        //   case SLEEP_YESTERDAY_TIME:
        //     trackerAPICallback.onSleepTime(false, 0, "");
        //     break;
        // }
      }
    }

    // public JSONObject getSoftwareVersion(byte[] bytes) {
    //   JSONObject response = new JSONObject();
    //   try {
    //       byte[] version = new byte[14];
    //       for (int i = 1; bytes[i] != 0x00 && i < 6; i++) {
    //         version[i - 1] = bytes[i];
    //       }
    //       String versionNumber = "0.0.0";

    //       try {
    //         versionNumber = new String(version, "UTF-8").trim();
    //       }
    //       catch(Exception ex) {
    //         versionNumber = "0.0.0";
    //       }

    //       response.put("version", versionNumber);
    //   }
    //   catch (JSONException e) { // this shouldn't happen
    //       LOG.e(TAG, "onSuccessCall: JSONException" + e);
    //       e.printStackTrace();
    //   }

    //   writeCallback.success(response);
    //   return response;
    // }

    void getSoftVersionResponse(byte[] response) {
      try {
        byte[] version = new byte[14];
        for (int i = 1; response[i] != 0x00 && i < 6; i++) {
          version[i - 1] = response[i];
        }
        String versionNumber = "0.0.0";
        versionNumber = new String(version, "UTF-8").trim();

        writeCallback.success(versionNumber);
        commandCompleted();
        // timeFromSync = 0;
        // Log.wtf("=======SOFT VERSION=======", new String(version, "UTF-8").trim());
        // trackerAPICallback.onConnect(true);
        // isConnected = true;
        // trackerAPICallback.onVersionNumber(true, new String(version, "UTF-8").trim());
      } catch (Exception ex) {
      }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        LOG.d(TAG, "onCharacteristicRead " + characteristic);

        if (readCallback != null) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCallback.success(characteristic.getValue());
            } else {
                readCallback.error("Error reading " + characteristic.getUuid() + " status=" + status);
            }

            LOG.d(TAG, "readCallback = null " + characteristic);
            readCallback = null;

        }

        commandCompleted();
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        LOG.d(TAG, "onCharacteristicWrite " + characteristic);

        // if (writeCallback != null) {

        //     if (status == BluetoothGatt.GATT_SUCCESS) {
                
        //     } else {
        //         writeCallback.error(status);
        //     }

        //     writeCallback = null;
        // }

        // commandCompleted();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        LOG.d(TAG, "onDescriptorWrite " + descriptor);
        commandCompleted();
    }


    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        if (readCallback != null) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateRssi(rssi);
                readCallback.success(rssi);
            } else {
                readCallback.error("Error reading RSSI status=" + status);
            }

            readCallback = null;
        }
        commandCompleted();
    }

    // Update rssi and scanRecord.
    public void update(int rssi, byte[] scanRecord) {
        this.advertisingRSSI = rssi;
        this.advertisingData = scanRecord;
    }

    public void updateRssi(int rssi) {
        advertisingRSSI = rssi;
    }

    // This seems way too complicated
    private void registerNotifyCallback(CallbackContext callbackContext, UUID serviceUUID, UUID characteristicUUID) {

        boolean success = false;

        if (gatt == null) {
            callbackContext.error("BluetoothGatt is null");
            return;
        }

        BluetoothGattService service = gatt.getService(serviceUUID);
        BluetoothGattCharacteristic characteristic = findNotifyCharacteristic(service, characteristicUUID);
        String key = generateHashKey(serviceUUID, characteristic);

        if (characteristic != null) {

            notificationCallbacks.put(key, callbackContext);

            if (gatt.setCharacteristicNotification(characteristic, true)) {

                // Why doesn't setCharacteristicNotification write the descriptor?
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID);
                if (descriptor != null) {

                    // prefer notify over indicate
                    if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    } else {
                        LOG.w(TAG, "Characteristic " + characteristicUUID + " does not have NOTIFY or INDICATE property set");
                    }

                    if (gatt.writeDescriptor(descriptor)) {
                        success = true;
                    } else {
                        callbackContext.error("Failed to set client characteristic notification for " + characteristicUUID);
                    }

                } else {
                    callbackContext.error("Set notification failed for " + characteristicUUID);
                }

            } else {
                callbackContext.error("Failed to register notification for " + characteristicUUID);
            }

        } else {
            callbackContext.error("Characteristic " + characteristicUUID + " not found");
        }

        if (!success) {
            commandCompleted();
        }
    }

    private void removeNotifyCallback(CallbackContext callbackContext, UUID serviceUUID, UUID characteristicUUID) {

        if (gatt == null) {
            callbackContext.error("BluetoothGatt is null");
            return;
        }

        BluetoothGattService service = gatt.getService(serviceUUID);
        BluetoothGattCharacteristic characteristic = findNotifyCharacteristic(service, characteristicUUID);
        String key = generateHashKey(serviceUUID, characteristic);

        if (characteristic != null) {

            notificationCallbacks.remove(key);

            if (gatt.setCharacteristicNotification(characteristic, false)) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID);
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
                callbackContext.success();
            } else {
                // TODO we can probably ignore and return success anyway since we removed the notification callback
                callbackContext.error("Failed to stop notification for " + characteristicUUID);
            }

        } else {
            callbackContext.error("Characteristic " + characteristicUUID + " not found");
        }

        commandCompleted();

    }

    // Some devices reuse UUIDs across characteristics, so we can't use service.getCharacteristic(characteristicUUID)
    // instead check the UUID and properties for each characteristic in the service until we find the best match
    // This function prefers Notify over Indicate
    private BluetoothGattCharacteristic findNotifyCharacteristic(BluetoothGattService service, UUID characteristicUUID) {
        BluetoothGattCharacteristic characteristic = null;

        // Check for Notify first
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }

        if (characteristic != null) return characteristic;

        // If there wasn't Notify Characteristic, check for Indicate
        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }

        // As a last resort, try and find ANY characteristic with this UUID, even if it doesn't have the correct properties
        if (characteristic == null) {
            characteristic = service.getCharacteristic(characteristicUUID);
        }

        return characteristic;
    }

    private void readCharacteristic(CallbackContext callbackContext, UUID serviceUUID, UUID characteristicUUID) {

        boolean success = false;

        if (gatt == null) {
            callbackContext.error("BluetoothGatt is null");
            return;
        }

        BluetoothGattService service = gatt.getService(serviceUUID);
        BluetoothGattCharacteristic characteristic = findReadableCharacteristic(service, characteristicUUID);

        if (characteristic == null) {
            callbackContext.error("Characteristic " + characteristicUUID + " not found.");
        } else {
            readCallback = callbackContext;
            if (gatt.readCharacteristic(characteristic)) {
                success = true;
            } else {
                readCallback = null;
                callbackContext.error("Read failed");
            }
        }

        if (!success) {
            commandCompleted();
        }

    }

    private void readRSSI(CallbackContext callbackContext) {

        boolean success = false;

        if (gatt == null) {
            callbackContext.error("BluetoothGatt is null");
            return;
        }

        readCallback = callbackContext;

        if (gatt.readRemoteRssi()) {
            success = true;
        } else {
            readCallback = null;
            callbackContext.error("Read RSSI failed");
        }

        if (!success) {
            commandCompleted();
        }

    }

    // Some peripherals re-use UUIDs for multiple characteristics so we need to check the properties
    // and UUID of all characteristics instead of using service.getCharacteristic(characteristicUUID)
    private BluetoothGattCharacteristic findReadableCharacteristic(BluetoothGattService service, UUID characteristicUUID) {
        BluetoothGattCharacteristic characteristic = null;

        int read = BluetoothGattCharacteristic.PROPERTY_READ;

        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & read) != 0 && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }

        // As a last resort, try and find ANY characteristic with this UUID, even if it doesn't have the correct properties
        if (characteristic == null) {
            characteristic = service.getCharacteristic(characteristicUUID);
        }

        return characteristic;
    }

    private void writeCharacteristic(CallbackContext callbackContext, UUID serviceUUID, UUID characteristicUUID, byte[] data, int writeType) {

        boolean success = false;

        if (gatt == null) {
            callbackContext.error("BluetoothGatt is null");
            return;
        }

        BluetoothGattService service = gatt.getService(serviceUUID);
        BluetoothGattCharacteristic characteristic = findWritableCharacteristic(service, characteristicUUID, writeType);

        if (characteristic == null) {
            callbackContext.error("Characteristic " + characteristicUUID + " not found.");
        } else {
            characteristic.setValue(data);
            characteristic.setWriteType(writeType);
            writeCallback = callbackContext;

            if (gatt.writeCharacteristic(characteristic)) {
                success = true;
            } else {
                writeCallback = null;
                callbackContext.error("Write failed");
            }
        }

        if (!success) {
            commandCompleted();
        }

    }

    // Some peripherals re-use UUIDs for multiple characteristics so we need to check the properties
    // and UUID of all characteristics instead of using service.getCharacteristic(characteristicUUID)
    private BluetoothGattCharacteristic findWritableCharacteristic(BluetoothGattService service, UUID characteristicUUID, int writeType) {
        BluetoothGattCharacteristic characteristic = null;

        // get write property
        int writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE;
        if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
        }

        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & writeProperty) != 0 && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }

        // As a last resort, try and find ANY characteristic with this UUID, even if it doesn't have the correct properties
        if (characteristic == null) {
            characteristic = service.getCharacteristic(characteristicUUID);
        }

        return characteristic;
    }

    public void queueRead(CallbackContext callbackContext, UUID serviceUUID, UUID characteristicUUID) {
        BLECommand command = new BLECommand(callbackContext, serviceUUID, characteristicUUID, BLECommand.READ);
        queueCommand(command);
    }

    public void queueWrite(CallbackContext callbackContext, UUID serviceUUID, UUID characteristicUUID, byte[] data, int writeType) {
        BLECommand command = new BLECommand(callbackContext, serviceUUID, characteristicUUID, data, writeType);
        queueCommand(command);
    }

    public void queueRegisterNotifyCallback(CallbackContext callbackContext, UUID serviceUUID, UUID characteristicUUID) {
        BLECommand command = new BLECommand(callbackContext, serviceUUID, characteristicUUID, BLECommand.REGISTER_NOTIFY);
        queueCommand(command);
    }

    public void queueRemoveNotifyCallback(CallbackContext callbackContext, UUID serviceUUID, UUID characteristicUUID) {
        BLECommand command = new BLECommand(callbackContext, serviceUUID, characteristicUUID, BLECommand.REMOVE_NOTIFY);
        queueCommand(command);
    }


    public void queueReadRSSI(CallbackContext callbackContext) {
        BLECommand command = new BLECommand(callbackContext, null, null, BLECommand.READ_RSSI);
        queueCommand(command);
    }

    // add a new command to the queue
    private void queueCommand(BLECommand command) {
        LOG.d(TAG,"Queuing Command " + command);
        commandQueue.add(command);

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        command.getCallbackContext().sendPluginResult(result);

        if (!bleProcessing) {
            processCommands();
        }
    }

    // command finished, queue the next command
    private void commandCompleted() {
        LOG.d(TAG,"Processing Complete");
        bleProcessing = false;
        processCommands();
    }

    // process the queue
    private void processCommands() {
        LOG.d(TAG,"Processing Commands");

        if (bleProcessing) { return; }

        BLECommand command = commandQueue.poll();
        if (command != null) {
            if (command.getType() == BLECommand.READ) {
                LOG.d(TAG,"Read " + command.getCharacteristicUUID());
                bleProcessing = true;
                readCharacteristic(command.getCallbackContext(), command.getServiceUUID(), command.getCharacteristicUUID());
            } else if (command.getType() == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) {
                LOG.d(TAG,"Write " + command.getCharacteristicUUID());
                bleProcessing = true;
                writeCharacteristic(command.getCallbackContext(), command.getServiceUUID(), command.getCharacteristicUUID(), command.getData(), command.getType());
            } else if (command.getType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                LOG.d(TAG,"Write No Response " + command.getCharacteristicUUID());
                bleProcessing = true;
                writeCharacteristic(command.getCallbackContext(), command.getServiceUUID(), command.getCharacteristicUUID(), command.getData(), command.getType());
            } else if (command.getType() == BLECommand.REGISTER_NOTIFY) {
                LOG.d(TAG,"Register Notify " + command.getCharacteristicUUID());
                bleProcessing = true;
                registerNotifyCallback(command.getCallbackContext(), command.getServiceUUID(), command.getCharacteristicUUID());
            } else if (command.getType() == BLECommand.REMOVE_NOTIFY) {
                LOG.d(TAG,"Remove Notify " + command.getCharacteristicUUID());
                bleProcessing = true;
                removeNotifyCallback(command.getCallbackContext(), command.getServiceUUID(), command.getCharacteristicUUID());
            } else if (command.getType() == BLECommand.READ_RSSI) {
                LOG.d(TAG,"Read RSSI");
                bleProcessing = true;
                readRSSI(command.getCallbackContext());
            } else {
                // this shouldn't happen
                throw new RuntimeException("Unexpected BLE Command type " + command.getType());
            }
        } else {
            LOG.d(TAG, "Command Queue is empty.");
        }

    }

    private String generateHashKey(BluetoothGattCharacteristic characteristic) {
        return generateHashKey(characteristic.getService().getUuid(), characteristic);
    }

    private String generateHashKey(UUID serviceUUID, BluetoothGattCharacteristic characteristic) {
        return String.valueOf(serviceUUID) + "|" + characteristic.getUuid() + "|" + characteristic.getInstanceId();
    }

}





// ADD THIS TO A NEW FILE 


// public class ActivityData extends Object {
//   String time;
//   int index;
//   float calories;
//   int steps;
//   float distance;

//   public ActivityData(byte[] data){
//       LOG.d(TAG, "ActivityData " + data);
//       time =  "20" ;
//       int year = Integer.valueOf(Integer.toString(data[2],16));
//       int month = Integer.valueOf(Integer.toString(data[3],16));
//       int day = Integer.valueOf(Integer.toString(data[4],16));
//       time += year < 10? "0" : "" + String.valueOf(year)+ "-";
//       time += month < 10? "0" : "";
//       time += String.valueOf(month) + "-";
//       time += day < 10? "0" : "";
//       time += String.valueOf(day);
//       index = data[5];
//       Date now = new Date();
//       time += "T" + new SimpleDateFormat("hh:mm:ss")
//               .format(new Date( data[5]*15*60000 -
//                       TimeZone.getDefault().getOffset(now.getTime())));
//       if( data[6] == 0x00 ){
//           calories = (float)( data[8] < 0 ? data[8] & 0xff : data[8]);
//           calories *= 256;
//           calories += (float)( data[7] < 0 ? data[7] & 0xff : data[7]);
//           calories = calories / 100;

//           steps = data[10] < 0 ? data[10] & 0xff : data[10];
//           steps *= 256;
//           steps += data[9] < 0 ? data[9] & 0xff : data[9];

//           distance = (float)( data[12] < 0 ? data[12] & 0xff : data[12]);
//           distance *= 256;
//           distance += (float)( data[11] < 0 ? data[11] & 0xff : data[11]);
//       }
//   }

//   public void print(){
//       LOG.d(TAG, "ActivityData", "Time: (" + time
//               + ") calories: (" + String.valueOf(calories)
//               + ") steps: (" + String.valueOf(steps)
//               + ") distance: (" + String.valueOf(distance)+")");
//   }

//   public String getTime() {
//       return time;
//   }

//   public int getIndex() {
//       return index;
//   }

//   public float getCalories() {
//       return calories;
//   }

//   public int getSteps() {
//       return steps;
//   }

//   public float getDistance() {
//       return distance;
//   }

// }



// ADD THIS TO A NEW FILE 

// public enum ActivityState {
//   DAY,
//   SUMMARY,
//   SUMMARY_SLEEP,
//   SLEEP_TIME,
//   SLEEP_YESTERDAY_TIME,
//   LATEST,
//   SLEEP
// }

// public class SleepData {
//   String time;
//   int index;
//   public int restfulness ;

//   public SleepData(byte[] data){
//       time =  "20" ;
//       int year = Integer.valueOf(Integer.toString(data[2],16));
//       int month = Integer.valueOf(Integer.toString(data[3],16));
//       int day = Integer.valueOf(Integer.toString(data[4],16));
//       time += year < 10? "0" : "" + String.valueOf(year)+ "-";
//       time += month < 10? "0" : "";
//       time += String.valueOf(month) + "-";
//       time += day < 10? "0" : "";
//       time += String.valueOf(day);

//       Date now = new Date();
//       time += "T" + new SimpleDateFormat("hh:mm:ss")
//               .format(new Date( data[5]*15*60000
//                      - TimeZone.getDefault().getOffset(now.getTime())));
//       index = data[5];
//       if( data[6] != 0x00 ){
//           restfulness = 0;
//           int restfulCount = 0;
//           for(int i=0; i<8; i++){
//               if(data[i+7]!=0) {
//                   restfulCount++;
//                   int sleepData = (data[i + 7] < 0 ?  128 : data[i + 7]);
//                   restfulness += 100.0 - ((sleepData) / 1.28);
//               }
//           }
//           restfulness = restfulCount != 0 ? restfulness/restfulCount : 0;
//       }
//   }

//   public void print(){
//     LOG.d(TAG,"SleepData", "Time: (" + time
//               + ") restfulness : (" + String.valueOf(restfulness)+")");
//   }
// }
