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

/* global cordova, module */
"use strict";

var stringToArrayBuffer = function(str) {
    var ret = new Uint8Array(str.length);
    for (var i = 0; i < str.length; i++) {
        ret[i] = str.charCodeAt(i);
    }
    // TODO would it be better to return Uint8Array?
    return ret.buffer;
};

var base64ToArrayBuffer = function(b64) {
    return stringToArrayBuffer(atob(b64));
};

function massageMessageNativeToJs(message) {
    if (message.CDVType == 'ArrayBuffer') {
        message = base64ToArrayBuffer(message.data);
    }
    return message;
}

// Cordova 3.6 doesn't unwrap ArrayBuffers in nested data structures
// https://github.com/apache/cordova-js/blob/94291706945c42fd47fa632ed30f5eb811080e95/src/ios/exec.js#L107-L122
function convertToNativeJS(object) {
    Object.keys(object).forEach(function (key) {
        var value = object[key];
        object[key] = massageMessageNativeToJs(value);
        if (typeof(value) === 'object') {
            convertToNativeJS(value);
        }
    });
}

module.exports = {

    scan: function (services, seconds, success, failure) {
        var successWrapper = function(peripheral) {
            convertToNativeJS(peripheral);
            success(peripheral);
        };
        cordova.exec(successWrapper, failure, 'BLE', 'scan', [services, seconds]);
    },

    startScan: function (services, success, failure) {
        var successWrapper = function(peripheral) {
            convertToNativeJS(peripheral);
            success(peripheral);
        };
        cordova.exec(successWrapper, failure, 'BLE', 'startScan', [services]);
    },

    stopScan: function (success, failure) {
        cordova.exec(success, failure, 'BLE', 'stopScan', []);
    },

    startScanWithOptions: function(services, options, success, failure) {
        var successWrapper = function(peripheral) {
            convertToNativeJS(peripheral);
            success(peripheral);
        };
        options = options || {};
        cordova.exec(successWrapper, failure, 'BLE', 'startScanWithOptions', [services, options]);
    },

    // this will probably be removed
    list: function (success, failure) {
        cordova.exec(success, failure, 'BLE', 'list', []);
    },

    connect: function (device_id, success, failure) {
        var successWrapper = function(peripheral) {
            convertToNativeJS(peripheral);
            success(peripheral);
        };
        cordova.exec(successWrapper, failure, 'BLE', 'connect', [device_id]);
    },

    disconnect: function (device_id, success, failure) {
        cordova.exec(success, failure, 'BLE', 'disconnect', [device_id]);
    },

    // characteristic value comes back as ArrayBuffer in the success callback
    read: function (device_id, service_uuid, characteristic_uuid, success, failure) {
        cordova.exec(success, failure, 'BLE', 'read', [device_id, service_uuid, characteristic_uuid]);
    },

    // RSSI value comes back as an integer
    readRSSI: function(device_id, success, failure) {
        cordova.exec(success, failure, 'BLE', 'readRSSI', [device_id]);
    },

    // value must be an ArrayBuffer
    write: function (device_id, service_uuid, characteristic_uuid, value, success, failure) {
        cordova.exec(success, failure, 'BLE', 'write', [device_id, service_uuid, characteristic_uuid, value]);
    },

    // value must be an ArrayBuffer
    writeWithoutResponse: function (device_id, service_uuid, characteristic_uuid, value, success, failure) {
        cordova.exec(success, failure, 'BLE', 'writeWithoutResponse', [device_id, service_uuid, characteristic_uuid, value]);
    },

    // value must be an ArrayBuffer
    writeCommand: function (device_id, service_uuid, characteristic_uuid, value, success, failure) {
        console.log("WARNING: writeCommand is deprecated, use writeWithoutResponse");
        cordova.exec(success, failure, 'BLE', 'writeWithoutResponse', [device_id, service_uuid, characteristic_uuid, value]);
    },

    // success callback is called on notification
    notify: function (device_id, service_uuid, characteristic_uuid, success, failure) {
        console.log("WARNING: notify is deprecated, use startNotification");
        cordova.exec(success, failure, 'BLE', 'startNotification', [device_id, service_uuid, characteristic_uuid]);
    },

    // success callback is called on notification
    startNotification: function (device_id, success, failure) {
        cordova.exec(success, failure, 'BLE', 'startNotification', [device_id]);
    },

    // success callback is called when the descriptor 0x2902 is written
    stopNotification: function (device_id, service_uuid, characteristic_uuid, success, failure) {
        cordova.exec(success, failure, 'BLE', 'stopNotification', [device_id, service_uuid, characteristic_uuid]);
    },

    isConnected: function (device_id, success, failure) {
        cordova.exec(success, failure, 'BLE', 'isConnected', [device_id]);
    },

    isEnabled: function (success, failure) {
        cordova.exec(success, failure, 'BLE', 'isEnabled', []);
    },

    enable: function (success, failure) {
        cordova.exec(success, failure, "BLE", "enable", []);
    },

    showBluetoothSettings: function (success, failure) {
        cordova.exec(success, failure, "BLE", "showBluetoothSettings", []);
    },

    startStateNotifications: function (success, failure) {
        cordova.exec(success, failure, "BLE", "startStateNotifications", []);
    },

    stopStateNotifications: function (success, failure) {
        cordova.exec(success, failure, "BLE", "stopStateNotifications", []);
    },

    setDeviceTime: function (device_id, date, success, error) {
        cordova.exec(success, error, 'BLE', 'setDeviceTime', [device_id, date]);
    },
      
    getDeviceTime:function (success, error) {
        cordova.exec(success, error, 'BLE', 'getDeviceTime');
    },
      
      /*
       * @param {info} object
       * @example {
       *   gender : string (male|female)
       *   age : int (years)
       *   height : int (cm)
       *   weight : int (kg)
       *   strideLength : int (cm)
       * }
       */
    setUserPersonalInfo: function (info, success, error) {
        cordova.exec(success, error, 'BLE', 'setUserPersonalInfo', [info]);
    },
      
    getUserPersonalInfo: function (success, error) {
        cordova.exec(success, error, 'BLE', 'getUserPersonalInfo');
    },
      
    getDetailedDayActivity: function (device_id, date, success, error) {
        cordova.exec(success, error, 'BLE', 'getDetailedDayActivity', [device_id, date]);
    },
      
      /*
       * @param {info} object
       * @example {
       *   date : {date} string yyyy-MM-dd
       *   deviceDate : {date} string "yyyy-MM-dd'T'HH:mm:ss"
       * }
       */
    getSummaryDaySleep: function (info, success, error) {
        cordova.exec(success, error, 'BLE', 'getSummaryDaySleep', [info]);
    },
      
      /*
       * @param {info} object
       * @example {
       *   date : {date} string yyyy-MM-dd
       *   deviceDate : {date} string "yyyy-MM-dd'T'HH:mm:ss"
       * }
       */
    getSummaryDayActivity: function (info, success, error) {
        cordova.exec(success, error, 'BLE', 'getSummaryDayActivity', [info]);
    },
      
      
    getLastActivity: function (success, error) {
        cordova.exec(success, error, 'BLE', 'getLastActivity');
    },
      
    getLastSleepActivity: function (success, error) {
        cordova.exec(success, error, 'BLE', 'getLastSleepActivity');
    },
      
      /*
       * @param {dailySteps} int
       */
    setTargetSteps: function (dailySteps, success, error) {
        cordova.exec(success, error, 'BLE', 'setTargetSteps', [dailySteps]);
    },
      
    getTargetSteps: function (success, error) {
        cordova.exec(success, error, 'BLE', 'getTargetSteps');
    },
      
      /*
       * @param {unit} string (mile|km)
       */
    setDistanceUnit: function (unit, success, error) {
        cordova.exec(success, error, 'BLE', 'setDistanceUnit', [unit]);
    },
      
    getDistanceUnit: function (success, error) {
        cordova.exec(success, error, 'BLE', 'getDistanceUnit');
    },
      
    getDevicesBatteryStatus: function (success, error) {
        cordova.exec(success, error, 'BLE', 'getDevicesBatteryStatus');
    },
      
    getSoftwareVersion: function (device_id, success, error) {
        cordova.exec(success, error, 'BLE', 'getSoftwareVersion', [device_id]);
    },
      
      /*
       * @param {duration} int (1-10)
       */

    activateVibration: function (device_id, duration, success, failure) {
        cordova.exec(success, failure, 'BLE', 'activateVibration', [device_id, duration]);
    },

      
      /*
       * @param {format} string (12|24)
       */
    setTimeFormat: function (device_id, data, success, error) {
        cordova.exec(success, error, 'BLE', 'setTimeFormat', [device_id, data]);
    },
      
    getTimeFormat: function (success, error) {
        cordova.exec(success, error, 'BLE', 'getTimeFormat');
    },
      
      /*
       * @param {name} string
       */
    setDeviceName: function (name, success, error) {
        cordova.exec(success, error, 'BLE', 'setDeviceName', [name]);
    },
      
    getDeviceName: function (success, error) {
        cordova.exec(success, error, 'BLE', 'getDeviceName');
    }
      
      /*
       * @param {message} string
       */
    // showMessage = function (message, success, error) {
    //     cordova.exec(success, error, 'BLE', 'showMessage', [message]);
    // },
      
      /*
       * @param {mode} string (activity|sleep)
       */
    // setMode: function (device_id, mode, success, error) {
    //     cordova.exec(success, error, 'BLE', 'setMode', [device_id, mode]);
    // },
      
    // getMode: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getMode');
    // },
      
    // getDataFromScales: function (deviceId, userProfile, success, error) {
    //     cordova.exec(success, error, 'BLE', 'getDataFromScales', [deviceId, userProfile]);
    // }
      
    //   /*
    //    * @param {info} object
    //    * @example {
    //    *   gender : string (male|female)
    //    *   age : int (years)
    //    *   height : int (cm)
    //    * }
    //    */
    // setUserProfileToScales: function (info, success, error) {
    //     cordova.exec(success, error, 'BLE', 'setUserProfileToScales', [info]);
    // },
      
    // setStopScales: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'setStopScales');
    // },
      
    // startLoggingAndroid: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'StartLogging');
    // },
      
    // getLastSleepTime: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getLastSleepTime');
    // }

};

