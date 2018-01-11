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
    startNotification: function (device_id, service_uuid, characteristic_uuid, success, failure) {
        cordova.exec(success, failure, 'BLE', 'startNotification', [device_id, service_uuid, characteristic_uuid]);
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

    // setDeviceTime: function (date, success, error) {
    //     cordova.exec(success, error, 'BLE', 'setDeviceTime', [date]);
    // },
      
    // getDeviceTime:function (success, error) {
    //     console.log('getDeviceTime')
    //     cordova.exec(success, error, 'BLE', 'getDeviceTime');
    // },
      
    //   /*
    //    * @param {info} object
    //    * @example {
    //    *   gender : string (male|female)
    //    *   age : int (years)
    //    *   height : int (cm)
    //    *   weight : int (kg)
    //    *   strideLength : int (cm)
    //    * }
    //    */
    // setUserPersonalInfo: function (info, success, error) {
    //     cordova.exec(success, error, 'BLE', 'setUserPersonalInfo', [info]);
    // },
      
    // getUserPersonalInfo: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getUserPersonalInfo');
    // },
      
    // getDetailedDayActivity: function (date, success, error) {
    //     cordova.exec(success, error, 'BLE', 'getDetailedDayActivity', [date]);
    // },
      
    //   /*
    //    * @param {info} object
    //    * @example {
    //    *   date : {date} string yyyy-MM-dd
    //    *   deviceDate : {date} string "yyyy-MM-dd'T'HH:mm:ss"
    //    * }
    //    */
    // getSummaryDaySleep: function (info, success, error) {
    //     cordova.exec(success, error, 'BLE', 'getSummaryDaySleep', [info]);
    // },
      
    //   /*
    //    * @param {info} object
    //    * @example {
    //    *   date : {date} string yyyy-MM-dd
    //    *   deviceDate : {date} string "yyyy-MM-dd'T'HH:mm:ss"
    //    * }
    //    */
    // getSummaryDayActivity: function (info, success, error) {
    //     cordova.exec(success, error, 'BLE', 'getSummaryDayActivity', [info]);
    // },
      
      
    // getLastActivity: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getLastActivity');
    // },
      
    // getLastSleepActivity: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getLastSleepActivity');
    // },
      
    //   /*
    //    * @param {dailySteps} int
    //    */
    // setTargetSteps: function (dailySteps, success, error) {
    //     cordova.exec(success, error, 'BLE', 'setTargetSteps', [dailySteps]);
    // },
      
    // getTargetSteps: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getTargetSteps');
    // },
      
    //   /*
    //    * @param {unit} string (mile|km)
    //    */
    // setDistanceUnit: function (unit, success, error) {
    //     cordova.exec(success, error, 'BLE', 'setDistanceUnit', [unit]);
    // },
      
    // getDistanceUnit: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getDistanceUnit');
    // },
      
    // getDevicesBatteryStatus: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getDevicesBatteryStatus');
    // },
      
    // getSoftwareVersion: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getSoftwareVersion');
    // },
      
    //   /*
    //    * @param {duration} int (1-10)
    //    */
    // activateVibration: function (device_id, duration, success, error) {
    //     cordova.exec(success, error, 'BLE', 'activateVibration', [device_id, duration]);
    // },

    activateVibration: function (device_id, success, failure) {
        cordova.exec(success, failure, 'BLE', 'activateVibration', [device_id]);
    },

      
    //   /*
    //    * @param {format} string (12|24)
    //    */
    // setTimeFormat: function (format, success, error) {
    //     cordova.exec(success, error, 'BLE', 'setTimeFormat', [format]);
    // },
      
    // getTimeFormat: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getTimeFormat');
    // },
      
    //   /*
    //    * @param {name} string
    //    */
    // setDeviceName: function (name, success, error) {
    //     cordova.exec(success, error, 'BLE', 'setDeviceName', [name]);
    // },
      
    // getDeviceName: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getDeviceName');
    // },
      
    //   /*
    //    * @param {message} string
    //    */
    // showMessage = function (message, success, error) {
    //     cordova.exec(success, error, 'BLE', 'showMessage', [message]);
    // },
      
    //   /*
    //    * @param {mode} string (activity|sleep)
    //    */
    // setMode: function (mode, success, error) {
    //     cordova.exec(success, error, 'BLE', 'setMode', [mode]);
    // },
      
    // getMode: function (success, error) {
    //     cordova.exec(success, error, 'BLE', 'getMode');
    // },
      
    // getDataFromScales: function (deviceId, userProfile, success, error) {
    //     cordova.exec(success, error, 'BLE', 'getDataFromScales', [deviceId, userProfile]);
    // },
      
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

module.exports.withPromises = {
    scan: module.exports.scan,
    startScan: module.exports.startScan,
    startScanWithOptions: module.exports.startScanWithOptions,
    connect: module.exports.connect,
    startNotification: module.exports.startNotification,
    startStateNotifications: module.exports.startStateNotifications,

    stopScan: function() {
        return new Promise(function(resolve, reject) {
            module.exports.stopScan(resolve, reject);
        });
    },

    disconnect: function(device_id) {
        return new Promise(function(resolve, reject) {
            module.exports.disconnect(device_id, resolve, reject);
        });
    },

    read: function(device_id, service_uuid, characteristic_uuid) {
        return new Promise(function(resolve, reject) {
            module.exports.read(device_id, service_uuid, characteristic_uuid, resolve, reject);
        });
    },

    write: function(device_id, service_uuid, characteristic_uuid, value) {
        return new Promise(function(resolve, reject) {
            module.exports.write(device_id, service_uuid, characteristic_uuid, value, resolve, reject);
        });
    },

    writeWithoutResponse: function (device_id, service_uuid, characteristic_uuid, value) {
        return new Promise(function(resolve, reject) {
            module.exports.writeWithoutResponse(device_id, service_uuid, characteristic_uuid, value, resolve, reject);
        });
    },

    stopNotification: function (device_id, service_uuid, characteristic_uuid) {
        return new Promise(function(resolve, reject) {
            module.exports.stopNotification(device_id, service_uuid, characteristic_uuid, resolve, reject);
        });
    },

    isConnected: function (device_id) {
        return new Promise(function(resolve, reject) {
            module.exports.isConnected(device_id, resolve, reject);
        });
    },

    isEnabled: function () {
        return new Promise(function(resolve, reject) {
            module.exports.isEnabled(resolve, reject);
        });
    },

    enable: function () {
        return new Promise(function(resolve, reject) {
            module.exports.enable(resolve, reject);
        });
    },

    showBluetoothSettings: function () {
        return new Promise(function(resolve, reject) {
            module.exports.showBluetoothSettings(resolve, reject);
        });
    },

    stopStateNotifications: function () {
        return new Promise(function(resolve, reject) {
            module.exports.stopStateNotifications(resolve, reject);
        });
    },

    readRSSI: function(device_id) {
        return new Promise(function(resolve, reject) {
            module.exports.readRSSI(device_id, resolve, reject);
        });
    }, 


    // setDeviceTime: function(device_id, date) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.setDeviceTime(device_id, date, resolve, reject);
    //     });
    // },

    // getDeviceTime: function(device_id) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getDeviceTime(device_id, resolve, reject);
    //     });
    // },

    // setUserPersonalInfo: function(device_id, info) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.setUserPersonalInfo(device_id, resolve, reject);
    //     });
    // },

    // getUserPersonalInfo: function(device_id) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.setDeviceTime(device_id, resolve, reject);
    //     });
    // },

    // getDetailedDayActivity: function(device_id, date) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getDetailedDayActivity(device_id, date, resolve, reject);
    //     });
    // },

    // getSummaryDaySleep: function(device_id, info) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getSummaryDaySleep(device_id, info, resolve, reject);
    //     });
    // },

    // getSummaryDayActivity: function(device_id, info) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getSummaryDayActivity(device_id, resolve, reject);
    //     });
    // },

    // getLastActivity: function(device_id) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getLastActivity(device_id, resolve, reject);
    //     });
    // },

    // getLastSleepActivity: function(device_id) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getLastSleepActivity(device_id, resolve, reject);
    //     });
    // },

    // setTargetSteps: function(device_id, data) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.setDeviceTime(device_id, data, resolve, reject);
    //     });
    // },

    // setDistanceUnit: function(device_id, data) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.setDistanceUnit(device_id, data, resolve, reject);
    //     });
    // },

    // getDistanceUnit: function(device_id) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getDistanceUnit(device_id, resolve, reject);
    //     });
    // },

    // getDevicesBatteryStatus: function(device_id) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getDevicesBatteryStatus(device_id, resolve, reject);
    //     });
    // },

    // getSoftwareVersion: function(device_id) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getSoftwareVersion(device_id, resolve, reject);
    //     });
    // },

    // activateVibration: function(device_id, data) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.activateVibration(device_id, data, resolve, reject);
    //     });
    // },

    activateVibration: function(device_id) {
        return new Promise(function(resolve, reject) {
            module.exports.activateVibration(device_id, resolve, reject);
        });
    },

    // setTimeFormat: function(device_id, data) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.setTimeFormat(device_id, resolve, reject);
    //     });
    // },

    // showMessage: function(device_id, data) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.showMessage(device_id, data, resolve, reject);
    //     });
    // },

    // setMode: function(device_id, data) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.setMode(device_id, resolve, reject);
    //     });
    // },

    // getMode: function(device_id) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getMode(device_id, resolve, reject);
    //     });
    // },

    // getDataFromScales: function(device_id, data) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getDataFromScales(device_id, data, resolve, reject);
    //     });
    // },

    // setUserProfileToScales: function(device_id, info) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.setUserProfileToScales(device_id, info, resolve, reject);
    //     });
    // },

    // setStopScales: function(device_id) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.setStopScales(device_id, resolve, reject);
    //     });
    // },

    // getLastSleepTime: function(device_id) {
    //     return new Promise(function(resolve, reject) {
    //         module.exports.getLastSleepTime(device_id, resolve, reject);
    //     });
    // },

};
