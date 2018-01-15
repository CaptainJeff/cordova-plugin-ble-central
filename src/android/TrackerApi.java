package com.megster.cordova.ble.central;

// import android.app.Activity;
// import android.bluetooth.BluetoothDevice;
// import android.os.Handler;
// import android.os.Looper;
// import android.util.Log;

// import com.polidea.rxandroidble.RxBleDevice;
// import com.polidea.rxandroidble.scan.ScanResult;

// import org.json.JSONArray;
// import org.json.JSONObject;

// import java.sql.Time;
// import java.text.SimpleDateFormat;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.Date;
// import java.util.List;
// import java.util.Timer;
// import java.util.TimerTask;
// import java.util.concurrent.TimeUnit;

// import static com.selvera.bluetooth.CommandCode.*;

// public class TrackerAPI {

//   ArrayList<ActivityData> dayActivity;
//   ArrayList<SleepData> daySleep;

//   TrackerAPICallback trackerAPICallback;

//   int dayActivityCount = 0;
//   String summaryDay = "";
//   int scaleResponseCount = 0;
//   BluetoothCore btManager;

//   // Timer timer;
//   int timeFromSync = 0;
//   int lastSleepTime = 0;
//   boolean isConnected = false;
//   ActivityState state = ActivityState.DAY;

//   public TrackerAPI(final Activity activity) {
//     btManager = new BluetoothCore(activity);
//     setCommunicationCallback();
//     /*timer = new Timer();
//     timer.scheduleAtFixedRate(new TimerTask() {
//       @Override
//       public void run() {
//         btManager.testLog();
//       }
//     }, 5000, 5000);*/
//   }

//   public static byte calcCRC(byte[] message) {
//     int crcSum = 0x00;
//     for (int i = 0; i < 15; i++) {
//       crcSum += message[i];
//     }
//     return (byte) (crcSum > 0xFF ? (byte) (crcSum % 256) & 0xFF : (byte) (crcSum) & 0xFF);
//   }

//   public void disconnect(String id) {
//     btManager.disconnectGatt(true);
//     /*if(btManager.mBluetoothGatt!=null && btManager.mBluetoothGatt.getDevice().getAddress().equalsIgnoreCase(id)) {
//       new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//         @Override
//         public void run() {
//           btManager.disconnectGatt(true);
//         }
//       }, 100);
//     }*/
//   }

//   public void setDateTime(String timeStamp) {
//     //final String timeStamp1 = timeStamp.replace('', ' ');
//     try {
//       byte[] message = new byte[16];
//       message[0] = setDeviceTime;
//       String b = "";
//       message[1] = Byte.parseByte(timeStamp.substring(2,4), 16);
//       message[2] = Byte.parseByte(timeStamp.substring(5,7), 16);
//       message[3] = Byte.parseByte(timeStamp.substring(8,10), 16);
//       message[4] = Byte.parseByte(timeStamp.substring(11,13), 16);
//       message[5] = Byte.parseByte(timeStamp.substring(14,16), 16);
//       message[6] = Byte.parseByte(timeStamp.substring(17,18), 16);
//       message[15] = calcCRC(message);
//       btManager.writeData(message);
//       timeFromSync = 0;
//     } catch (Exception ex) {
//     }
//   }

//   public void getCurrentTime() {
//     timeFromSync = 0;
//     baseRequest(getDeviceTime);
//   }

//   public void getCurrentTimeResponse(byte[] response) {
//     String time = "20";
//     int year = Integer.valueOf(Integer.toString(response[1], 16));
//     int month = Integer.valueOf(Integer.toString(response[2], 16));
//     int day = Integer.valueOf(Integer.toString(response[3], 16));
//     time += year < 10 ? "0" : "";
//     time += String.valueOf(year) + "-";
//     time += month < 10 ? "0" : "";
//     time += String.valueOf(month) + "-";
//     time += day < 10 ? "0" : "";
//     time += String.valueOf(day);
//     time += "T";
//     int hour = Integer.valueOf(Integer.toString(response[4], 16));
//     int minute = Integer.valueOf(Integer.toString(response[5], 16));
//     int second = Integer.valueOf(Integer.toString(response[6], 16));
//     time += hour < 10 ? "0" : "";
//     time += String.valueOf(hour) + ":";
//     time += minute < 10 ? "0" : "";
//     time += String.valueOf(minute) + ":";
//     time += second < 10 ? "0" : "";
//     time += String.valueOf(second);
//     timeFromSync = 0;
//     Log.d("TIME: ", time);
//     trackerAPICallback.onGetTime(true, time);
//   }

//   public void bind() {
//     baseRequest((byte) 0x20);
//     timeFromSync = 0;
//   }

//   public void setTimeFormat(String timeFormat) {
//     byte[] message = new byte[16];
//     message[0] = setTimeFormat;
//     message[1] = (byte) (timeFormat == "12" ? 0x00 : 0x01);
//     message[15] = calcCRC(message);
//     timeFromSync = 0;
//     btManager.writeData(message);
//   }

//   public void getLastSleepTime() {
//     lastSleepTime = 0;
//     state = ActivityState.SLEEP_TIME;
//     getDayActivity(0);
//   }

//   public void vibrate(final int duration) {
//     Log.d("ACTION", "VIBRATE");
//     byte[] message = new byte[16];
//     message[0] = activateVibration;
//     message[1] = (byte) (duration > 10 ? 10 : duration);
//     message[15] = calcCRC(message);
//     timeFromSync = 0;
//     btManager.writeData(message);
//   }

//   public void resetDevice() {
//     baseRequest((byte) 0x12);
//   }

//   public void getTargetSteps() {
//     baseRequest(getTargetSteps);
//   }

//   public void setUserPersonalInfo(final String gender, final int age, final int height, final int weight, final int strideLength) {
//     byte[] message = new byte[16];
//     message[0] = setUserPersonalInfo;
//     message[1] = (byte) (gender == "male" ? 1 : 0);
//     message[2] = (byte) age;
//     message[3] = (byte) height;
//     message[4] = (byte) weight;
//     message[5] = (byte) strideLength;
//     message[15] = calcCRC(message);
//     timeFromSync = 0;
//     btManager.writeData(message);
//   }

//   public void getUsetPersonalInfo() {
//     byte[] message = new byte[16];
//     message[0] = getUserPersonalInfo;
//     message[15] = calcCRC(message);
//     timeFromSync = 0;
//     btManager.writeData(message);
//   }

//   public void getUserPersonalInfoResponse(final byte[] response) {
//     String[] result = new String[5];
//     result[0] = "gender: " + (response[1] == 0x00 ? "female" : "male");
//     result[1] = "age: " + String.valueOf(response[5] < 0 ? response[5] & 0xff : response[5]);
//     result[2] = "height: " + String.valueOf(response[3] < 0 ? response[3] & 0xff : response[3]);
//     result[3] = "weight: " + String.valueOf(response[4] < 0 ? response[4] & 0xff : response[4]);
//     result[4] = "stride length: " + String.valueOf(response[5] < 0 ? response[5] & 0xff : response[5]);
//     try {
//       JSONArray array = new JSONArray();
//       array.put(result[0]);
//       array.put(result[1]);
//       array.put(result[2]);
//       array.put(result[3]);
//       array.put(result[4]);
//       timeFromSync = 0;
//       trackerAPICallback.onGetUserPersonalData(true, array.toString());
//     } catch (Exception ex) {
//     }
//   }

//   public void setDistanceUnit(final String unit) {
//     byte[] message = new byte[16];
//     message[0] = setDistanceUnit;
//     message[1] = (byte) (unit == "KM" ? 0x00 : 0x01);
//     message[15] = calcCRC(message);
//     timeFromSync = 0;
//     btManager.writeData(message);
//   }

//   public void getDistanceUnit() {
//     byte[] message = new byte[16];
//     message[0] = getDistanceUnit;
//     message[15] = calcCRC(message);
//     timeFromSync = 0;
//     btManager.writeData(message);
//   }

//   private void getDistanceUnitResponse(byte[] response) {
//     String unit = "";
//     switch (response[1]) {
//       case 0x00:
//         unit = "KM";
//         break;
//       case 0x01:
//         unit = "Mile";
//         break;
//       case 0x02:
//         unit = "CM";
//         break;
//       default:
//         unit = "Mile";
//         break;
//     }
//     timeFromSync = 0;
//     trackerAPICallback.onDistanceUnitResponse(true, unit);
//   }

//   public void switchTOActivityOrSleepMode(String mode) {
//     final byte[] message = new byte[16];
//     message[0] = setMode;
//     message[1] = (mode.equalsIgnoreCase("sleep") ? (byte) 0x01 : (byte) 0x02);
//     message[15] = calcCRC(message);
//     timeFromSync = 0;
//     btManager.writeData(message);
//   }

//   public void getActivityOrSleepMode() {
//     baseRequest(getMode);
//   }

//   public void getActivityOrSleepModeResponce(byte[] response) {
//     if(response[1]==0x01){
//       trackerAPICallback.onModeResponse(true, "activity");
//     } else if(response[1]==0x02) {
//       trackerAPICallback.onModeResponse(true, "sleep");
//     } else {
//       trackerAPICallback.onModeResponse(true, "unknown");
//     }
//   }

//   public void getBatteryStatus() {
//     baseRequest(getDevicesBatteryStatus);
//   }

//   public void getSoftVersion() {
//     baseRequest(getSoftwareVersion);
//   }

//   void getSoftVersionResponse(byte[] response) {
//     try {
//       byte[] version = new byte[14];
//       for (int i = 1; response[i] != 0x00 && i < 6; i++) {
//         version[i - 1] = response[i];
//       }
//       String versionNumber = new String(version, "UTF-8").trim();
//       timeFromSync = 0;
//       Log.wtf("=======SOFT VERSION=======", versionNumber);
//       trackerAPICallback.onConnect(true);
//       isConnected = true;
//       trackerAPICallback.onVersionNumber(true, versionNumber);
//     } catch (Exception ex) {
//     }
//   }

//   public void setDeviceName(String name) {
//     byte[] message = new byte[16];
//     try {
//       message[0] = setDeviceName;
//       byte[] nameBytes = name.getBytes("UTF-8");
//       for (int i = 0; i < (nameBytes.length > 14 ? 14 : nameBytes.length); i++) {
//         message[i + 1] = nameBytes[i];
//       }
//       message[15] = calcCRC(message);
//       timeFromSync = 0;
//       btManager.writeData(message);
//     } catch (Exception ex) {
//     }
//   }

//   public void getDeviceName() {
//     baseRequest(getDeviceName);
//   }

//   public void getTimeFormat() {
//     baseRequest(getTimeFormat);
//   }

//   private void getTimeFormatResponse(byte[] response) {
//     String format;
//     format = response[1] == 0x00 ? "12" : "24";
//     timeFromSync = 0;
//     trackerAPICallback.onGetTimeFormat(true, format);
//   }

//   public void getDayActivity(String date) {
//     state = ActivityState.DAY;
//     getDayActivity(dateToDayIndex(date));
//     timeFromSync = 0;
//   }

//   public void getDayActivity(final int day) {
//     dayActivity = new ArrayList<ActivityData>();
//     daySleep = new ArrayList<SleepData>();
//     byte[] message = new byte[16];
//     message[0] = getDetailedCurrentDayActivityData;
//     message[1] = (byte) day;
//     message[15] = calcCRC(message);
//     timeFromSync = 0;
//     btManager.writeData(message);
//   }

//   private void dayActivityResponse(byte[] response) {
//     timeFromSync = 0;
//     ArrayList<SleepData> lastSleepData;
//     if (response[6] != (byte) (0xff)) {
//       dayActivity.add(new ActivityData(response));
//     } else {
//       daySleep.add(new SleepData(response));
//     }
//     if (response[1] != (byte) 0xff) {
//       if (dayActivity.size() + daySleep.size() == 96) {
//         Log.wtf("Error", String.valueOf(dayActivity.size() + daySleep.size()));
//         switch (state) {
//           case SUMMARY: {
//             int totalSteps = 0;
//             float totalCal = 0;
//             float totalDistanse = 0;
//             for (int i = 0; i < dayActivity.size(); i++) {
//               totalSteps += dayActivity.get(i).getSteps();
//               totalCal += dayActivity.get(i).calories;
//               totalDistanse += dayActivity.get(i).distance;
//             }
//             trackerAPICallback.onSummaryResponse(true, summaryDay, totalSteps, totalCal, totalDistanse);
//             break;
//           }
//           case DAY:
//             trackerAPICallback.onDailyActivityResponse(true, dayActivity.toArray(new ActivityData[dayActivity.size()]));
//             break;
//           case LATEST: {
//             int totalSteps = 0;
//             float totalCal = 0;
//             float totalDistanse = 0;
//             String date = "";
//             for (int i = 0; i < dayActivity.size(); i++) {
//               totalSteps += dayActivity.get(i).getSteps();
//               totalCal += dayActivity.get(i).calories;
//               totalDistanse += dayActivity.get(i).distance;
//               if (dayActivity.get(i).getSteps() != 0) {
//                 date = dayActivity.get(i).getTime();
//               }
//             }
//             trackerAPICallback.onLatestActivityResponse(true, date, totalSteps, totalCal, totalDistanse);
//             break;
//           }
//           case SLEEP: {
//             trackerAPICallback.onLastSleepResponse(true, daySleep.toArray(new SleepData[daySleep.size()]));
//             lastSleepData = daySleep;
//             //calcSleepTime();
//             break;
//           }
//           case SUMMARY_SLEEP: {
//             lastSleepData = daySleep;
//             //calcSleepTime();
//             trackerAPICallback.onSummarySleepResponse(true, getSleepFrames(daySleep.toArray(new SleepData[daySleep.size()])));
//             break;
//           }
//           case SLEEP_TIME: {
//             Log.wtf("SLEEP_TIME", "");
//             lastSleepData = daySleep;
//             calcSleepTime();
//             break;
//           }
//           case SLEEP_YESTERDAY_TIME: {
//             lastSleepData = daySleep;
//             calcSleepTime();
//             break;
//           }
//         }
//       } else {
//         Log.wtf("Error", String.valueOf(dayActivity.size() + daySleep.size()));
//       }
//     } else {
//       switch (state) {
//         case SUMMARY:
//           trackerAPICallback.onSummaryResponse(true, summaryDay, 0, 0, 0);
//           break;
//         case DAY:
//           trackerAPICallback.onDailyActivityResponse(false, null);
//           break;
//         case LATEST:
//           trackerAPICallback.onDailyActivityResponse(false, null);
//           break;
//         case SLEEP:
//           trackerAPICallback.onLastSleepResponse(false, null);
//           break;
//         case SUMMARY_SLEEP:
//           trackerAPICallback.onSummarySleepResponse(false, null);
//           break;
//         case SLEEP_TIME:
//           trackerAPICallback.onSleepTime(false, 0, "");
//           break;
//         case SLEEP_YESTERDAY_TIME:
//           trackerAPICallback.onSleepTime(false, 0, "");
//           break;
//       }
//     }
//   }

//   private void calcSleepTime() {
//     ArrayList<SleepData> lastSleepData;
//     lastSleepData = daySleep;
//     Collections.reverse(lastSleepData);
//     int i = 1;
//     if (lastSleepData.size() > 1) {
//       lastSleepTime += 15;
//       while (lastSleepData.get(i).index + 1 == lastSleepData.get(i - 1).index  && i <= lastSleepData.size()) {
//         lastSleepTime += 15;
//         i++;
//       }
//       if (lastSleepData.get(i).index == 0 && state == ActivityState.SLEEP_TIME) {
//         state = ActivityState.SLEEP_YESTERDAY_TIME;
//         Log.wtf("SLEEP", String.valueOf(lastSleepTime * 60));
//         getDayActivity(1);
//       } else {
//         trackerAPICallback.onSleepTime(true, lastSleepTime * 60, lastSleepData.get(i).time);
//       }
//       Log.wtf("SLEEP", String.valueOf(lastSleepTime * 60));
//     }
//   }

//   public JSONArray getSleepFrames(SleepData[] sleep) {
//     JSONArray sleepData = new JSONArray();
//     try {
//       SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//       long prevTime = sdf.parse(sleep[0].time.replace("T", " ")).getTime() / 60000;
//       JSONArray sleepFrame = new JSONArray();
//       for (int i = 0; i < sleep.length; i++) {
//         long curTime = sdf.parse(sleep[i].time.replace("T", " ")).getTime() / 60000;
//         if (curTime - prevTime <= 15) {
//           JSONObject sleepInterval = new JSONObject();
//           sleepInterval.put("time", sleep[i].time);
//           sleepInterval.put("quality", String.valueOf(sleep[i].restfulness));
//           sleepFrame.put(sleepInterval);
//         } else {
//           sleepData.put(sleepFrame);
//           sleepFrame = new JSONArray();
//           JSONObject sleepInterval = new JSONObject();
//           sleepInterval.put("time", sleep[i].time);
//           sleepInterval.put("quality", String.valueOf(sleep[i].restfulness));
//           sleepFrame.put(sleepInterval);
//         }
//         if (i == sleep.length - 1) {
//           sleepData.put(sleepFrame);
//         }
//         prevTime = curTime;
//       }
//       Log.d("SLEEP: ", sleepData.toString());
//     } catch (Exception ex) {
//       Log.d("Sleep: ", ex.getMessage());
//     }
//     return sleepData;
//   }


//   public void getDaySummary(String day) {
//     try {
//       timeFromSync = 0;
//       int dayIndex = dateToDayIndex(day);
//       state = ActivityState.SUMMARY;
//       summaryDay = day;
//       getDayActivity(dayIndex < 10 ? dayIndex : 29);
//     } catch (Exception ex) {
//     }
//   }

//   public void getLatestActivity() {
//     timeFromSync = 0;
//     state = ActivityState.LATEST;
//     getDayActivity(0);
//   }

//   int dateToDayIndex(String day) {
//     try {
//       timeFromSync = 0;
//       Date currentDay = new Date();
//       SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//       long timeDiff = currentDay.getTime() - sdf.parse(day).getTime();
//       return (int) (TimeUnit.DAYS.convert(timeDiff, TimeUnit.MILLISECONDS));
//     } catch (Exception ex) {
//       return 0;

//     }
//   }

//   public void setTargetSteps(int targetSteps) {
//     byte[] message = new byte[16];
//     message[0] = setTargetSteps;
//     byte byte1 = (byte) (targetSteps / 256 / 256);
//     targetSteps -= 256 * 256 * byte1;
//     byte byte2 = (byte) (targetSteps / 256);
//     targetSteps -= byte2 * 256;
//     byte byte3 = (byte) targetSteps;
//     message[1] = byte1;
//     message[2] = byte2;
//     message[3] = byte3;
//     message[15] = calcCRC(message);
//     timeFromSync = 0;
//     btManager.writeData(message);
//   }

//   public void parseResponse(byte[] response) {
//     byte func = response[0];
//     timeFromSync = 0;
//     parseFailResponce(response);
//     parseDataResponse(response);
//     switch (func) {
//       case setDeviceTime:
//         trackerAPICallback.onSetTime(true);
//         break;

//       case setUserPersonalInfo:
//         trackerAPICallback.onSetUserPersonalData(true);
//         break;
//       case setTargetSteps:
//         trackerAPICallback.onSetTargetSteps(true);
//         break;
//       case 0x20: //Log.d("BINDING_STATE", "ready");
//         break;
//       case 0x21: //Log.d("BINDING_STATE", "complite");
//         break;
//       case setTimeFormat:
//         trackerAPICallback.onSetTimeFormat(true);
//         break;
//       case setDistanceUnit:
//         trackerAPICallback.onSetDistanceUnitResponse(true);
//         break;
//       case setMode:
//         trackerAPICallback.onSetMode(true);
//         break;
//       case setModeError:
//         trackerAPICallback.onSetMode(false);
//       case setDeviceName:
//         trackerAPICallback.onSetTime(true);
//         break;
//       case activateVibration:
//         trackerAPICallback.onVibration(true);
//         break;
//       case 0x00:
//         scaleResponseCount++;
//         Log.d("SCALS RESPONSE", String.valueOf(response[2]));
//         if (scaleResponseCount > 10) {
//         }
//         break;
//       case (byte) 0xFB:
//         Log.d("0xFB", "result: " + String.valueOf(response[2]));
//         break;
//       case (byte) 0xCB:
//         Log.d("0xFC", "result");
//         break;
//       case (byte) 0xff:
//         break;
//       default:
//         break;
//     }
//   }

//   void parseFailResponce(byte[] response) {
//     timeFromSync = 0;
//     switch (response[0]) {
//       case getDeviceTimeError:
//         trackerAPICallback.onGetTime(false, "");
//         break;
//       case setUserPersonalInfoError:
//         trackerAPICallback.onSetUserPersonalData(false);
//         break;
//       case getUserPersonalInfoError:
//         trackerAPICallback.onGetUserPersonalData(false, "");
//         break;
//       case getDetailedCurrentDayActivityDataError:
//         trackerAPICallback.onDailyActivityResponse(false, null);
//         break;
//       case getTargetStepsError:
//         trackerAPICallback.onGetTargetSteps(false, 0);
//         break;
//       case setDeviceNameError:
//         trackerAPICallback.onSetDeviceName(false);
//         break;
//       case setTimeFormatError:
//         trackerAPICallback.onSetTimeFormat(false);
//         break;
//       case setModeError:
//         trackerAPICallback.onSetMode(false);
//         break;
//       case getDevicesBatteryStatusError:
//         trackerAPICallback.onBattery(false, 0);
//         break;
//       case getSoftwareVersionError:
//         trackerAPICallback.onVersionNumber(false, "");
//         break;
//       case activateVibrationError:
//         trackerAPICallback.onVibration(false);
//         break;
//       case getTimeFormatError:
//         trackerAPICallback.onGetTimeFormat(false, "");
//         break;
//       case getDeviceNameError:
//         trackerAPICallback.onGetDeviceName(false, "");
//         break;
//       case getModeError:
//         trackerAPICallback.onModeResponse(false, "");
//         break;
//       case setDistanceUnitError:
//         trackerAPICallback.onSetDistanceUnitResponse(false);
//         break;
//       case getDistanceUnitError:
//         trackerAPICallback.onDistanceUnitResponse(false, "");
//         break;
//       case (byte) 0xA1:
//         Log.d("BINDING_STATE", "fail");
//         break;
//       case (byte) 0xA0:
//         Log.d("BINDING_STATE", "fail");
//         break;
//       case setDeviceTimeError:
//         trackerAPICallback.onSetTime(false);
//         break;
//       case setTargetStepsError:
//         trackerAPICallback.onSetTargetSteps(false);
//         break;
//     }
//   }

//   void parseDataResponse(final byte[] response) {
//     timeFromSync = 0;
//     switch (response[0]) {
//       case getDevicesBatteryStatus:
//         batteryResponse(response);
//         break;
//       case getTargetSteps:
//         getTargetStepsResponse(response);
//         break;
//       case getSoftwareVersion:
//         getSoftVersionResponse(response);
//         break;
//       case getDeviceName:
//         getDeviceNameResponce(response);
//         break;
//       case getTimeFormat:
//         getTimeFormatResponse(response);
//         break;
//       case getDeviceTime:
//         getCurrentTimeResponse(response);
//         break;
//       case getUserPersonalInfo:
//         getUserPersonalInfoResponse(response);
//         break;
//       case getDetailedCurrentDayActivityData:
//         dayActivityResponse(response);
//         break;
//       case getDistanceUnit:
//         getDistanceUnitResponse(response);
//         break;
//       case getMode:
//         getActivityOrSleepModeResponce(response);
//         break;
//     }
//   }

//   public void batteryResponse(byte[] response) {
//     timeFromSync = 0;
//     float percent = 100f / 9;
//     Log.d("Battery", String.valueOf((int) (response[1] * percent)) + "%");
//     trackerAPICallback.onBattery(true, (int) percent);
//   }

//   public void getDeviceNameResponce(byte[] response) {
//     try {
//       timeFromSync = 0;
//       byte[] name = new byte[14];
//       for (int i = 1; response[i] != 0x00 && i < 15; i++) {
//         name[i - 1] = response[i];
//       }
//       trackerAPICallback.onGetDeviceName(true, new String(name, "UTF-8").trim());
//     } catch (Exception ex) {
//     }
//   }

//   public int getTargetStepsResponse(byte[] response) {
//     timeFromSync = 0;
//     int targetSteps = 0;
//     targetSteps = (response[1] < 0 ? response[1] & 0xff : response[1]) * 256 * 256;
//     targetSteps += (response[2] < 0 ? response[2] & 0xff : response[2]) * 256;
//     targetSteps += response[3] < 0 ? response[3] & 0xff : response[3];
//     trackerAPICallback.onGetTargetSteps(true, targetSteps);
//     return targetSteps;
//   }

//   private void setCommunicationCallback() {
//     btManager.setCommunicationCallback(new BluetoothCore.CommunicationCallback() {
//       @Override
//       public void onScanFinish(ArrayList<RxBleDevice> device, boolean isScaleConnect) {
//         trackerAPICallback.onFinish(device);
//       }

//       @Override
//       public void onConnect(RxBleDevice device) {
//         Log.d("onConnect", "device");
//         if (device != null) {
//           Log.d("onConnect", "not null");
//           getSoftVersion();
//         } else {
//           Log.d("onConnect", "onConnect = false");
//           trackerAPICallback.onConnect(false);
//           isConnected = false;
//         }
//       }

//       @Override
//       public void onStartSession() {
//         trackerAPICallback.onStartSession();
//       }

//       @Override
//       public void onDisconnect(BluetoothDevice device, String message) {
//         trackerAPICallback.onDisconnect(message);
//         btManager.closeGatt();
//       }

//       @Override
//       public void onMessage(byte[] message) {
//         parseResponse(message);
//       }

//       @Override
//       public void onError(String message) {
//       }

//       @Override
//       public void onConnectError(BluetoothDevice device, String message) {

//       }

//       @Override
//       public void sendLog(String log) {
//         trackerAPICallback.sendLog(log);
//       }
//     });
//   }

//   void baseRequest(final byte requestCode) {
//     timeFromSync = 0;
//     byte[] message = new byte[16];
//     message[0] = requestCode;
//     message[15] = calcCRC(message);
//     btManager.writeData(message);
//   }

//   public boolean connectToTracker(String id) {
//     Log.d("Tracker - connectToTracker: ", id);
//     timeFromSync = 0;
//     //if (btManager.getFoundedDevices().size() > 0) {
//     //  return btManager.connectGatt(id);
//     //} else {
//     btManager.scanDevices(id);
//     return false;
//     //}
//   }

//   public void stopScan() {
//     btManager.stopScan();
//   }

//   public void getSleepData() {
//     timeFromSync = 0;
//     state = ActivityState.SLEEP;
//     getDayActivity(1);
//   }

//   public void getSleepSummary(String date) {
//     timeFromSync = 0;
//     state = ActivityState.SUMMARY_SLEEP;
//     getDayActivity(dateToDayIndex(date));
//   }

//   public void startSession() {
//     timeFromSync = 0;
//     btManager.discoverServices();
//   }

//   public void scanDevices() {
//     timeFromSync = 0;
//     btManager.enableBluetooth();
//     btManager.scanDevices("");
//   }

//   public void setTrackerAPICallback(TrackerAPICallback trackerAPICallback) {
//     timeFromSync = 0;
//     this.trackerAPICallback = trackerAPICallback;
//   }

//   public enum ActivityState {
//     DAY,
//     SUMMARY,
//     SUMMARY_SLEEP,
//     SLEEP_TIME,
//     SLEEP_YESTERDAY_TIME,
//     LATEST,
//     SLEEP
//   }

//   public interface TrackerAPICallback {
//     void onFinish(ArrayList<RxBleDevice> device);

//     void onConnect(boolean success);

//     void onDisconnect(String state);

//     void onStartSession();

//     void onVibration(boolean result);

//     void onBattery(boolean success, int battery);

//     void onDailyActivityResponse(boolean success, ActivityData[] activity);

//     void onLastSleepResponse(boolean success, SleepData[] sleepData);

//     void onSummarySleepResponse(boolean success, JSONArray sleepData);

//     void onSummaryResponse(boolean success, String date, int steps, float calories, float distance);

//     void onLatestActivityResponse(boolean success, String date, int steps, float calories, float distance);

//     void onSetTargetSteps(boolean success);

//     void onGetTargetSteps(boolean success, int dailySteps);

//     void onVersionNumber(boolean success, String version);

//     void onSetDeviceName(boolean success);

//     void onGetDeviceName(boolean success, String name);

//     void onSetTimeFormat(boolean success);

//     void onGetTimeFormat(boolean success, String timeFormat);

//     void onSetTime(boolean success);

//     void onGetTime(boolean success, String time);

//     void onSetUserPersonalData(boolean success);

//     void onGetUserPersonalData(boolean success, String message);

//     void onSetMode(boolean success);

//     void onModeResponse(boolean success, String mode);

//     void onSetDistanceUnitResponse(boolean success);

//     void onDistanceUnitResponse(boolean success, String unit);

//     void onError(String error);

//     void sendLog(String log);

//     void onSleepTime(boolean success, int sleepTime, String from);
//   }
// }

