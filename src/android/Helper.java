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

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import org.json.JSONArray;

public class Helper {

    public static JSONArray decodeProperties(BluetoothGattCharacteristic characteristic) {

        // NOTE: props strings need to be consistent across iOS and Android
        JSONArray props = new JSONArray();
        int properties = characteristic.getProperties();

        if ((properties & BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0x0 ) {
            props.put("Broadcast");
        }

        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0x0 ) {
            props.put("Read");
        }

        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0x0 ) {
            props.put("WriteWithoutResponse");
        }

        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0x0 ) {
            props.put("Write");
        }

        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0x0 ) {
            props.put("Notify");
        }

        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0x0 ) {
            props.put("Indicate");
        }

        if ((properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0x0 ) {
            // Android calls this "write with signature", using iOS name for now
            props.put("AuthenticateSignedWrites");
        }

        if ((properties & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) != 0x0 ) {
            props.put("ExtendedProperties");
        }

//      iOS only?
//
//            if ((p & CBCharacteristicPropertyNotifyEncryptionRequired) != 0x0) {  // 0x100
//                [props addObject:@"NotifyEncryptionRequired"];
//            }
//
//            if ((p & CBCharacteristicPropertyIndicateEncryptionRequired) != 0x0) { // 0x200
//                [props addObject:@"IndicateEncryptionRequired"];
//            }

        return props;
    }

    public static JSONArray decodePermissions(BluetoothGattCharacteristic characteristic) {

        // NOTE: props strings need to be consistent across iOS and Android
        JSONArray props = new JSONArray();
        int permissions = characteristic.getPermissions();

        if ((permissions & BluetoothGattCharacteristic.PERMISSION_READ) != 0x0 ) {
            props.put("Read");
        }

        if ((permissions & BluetoothGattCharacteristic.PERMISSION_WRITE) != 0x0 ) {
            props.put("Write");
        }

        if ((permissions & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED) != 0x0 ) {
            props.put("ReadEncrypted");
        }

        if ((permissions & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED) != 0x0 ) {
            props.put("WriteEncrypted");
        }

        if ((permissions & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM) != 0x0 ) {
            props.put("ReadEncryptedMITM");
        }

        if ((permissions & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM) != 0x0 ) {
            props.put("WriteEncryptedMITM");
        }

        if ((permissions & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED) != 0x0 ) {
            props.put("WriteSigned");
        }

        if ((permissions & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM) != 0x0 ) {
            props.put("WriteSignedMITM");
        }

        return props;
    }

    public static JSONArray decodePermissions(BluetoothGattDescriptor descriptor) {

        // NOTE: props strings need to be consistent across iOS and Android
        JSONArray props = new JSONArray();
        int permissions = descriptor.getPermissions();

        if ((permissions & BluetoothGattDescriptor.PERMISSION_READ) != 0x0 ) {
            props.put("Read");
        }

        if ((permissions & BluetoothGattDescriptor.PERMISSION_WRITE) != 0x0 ) {
            props.put("Write");
        }

        if ((permissions & BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED) != 0x0 ) {
            props.put("ReadEncrypted");
        }

        if ((permissions & BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED) != 0x0 ) {
            props.put("WriteEncrypted");
        }

        if ((permissions & BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) != 0x0 ) {
            props.put("ReadEncryptedMITM");
        }

        if ((permissions & BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM) != 0x0 ) {
            props.put("WriteEncryptedMITM");
        }

        if ((permissions & BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED) != 0x0 ) {
            props.put("WriteSigned");
        }

        if ((permissions & BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM) != 0x0 ) {
            props.put("WriteSignedMITM");
        }

        return props;
    }

    public class CommandCode {
        static final byte setDeviceTime = 0x01;
        static final byte setDeviceTimeError = (byte)0x81;

        static final byte getDeviceTime = 0x41;
        static final byte getDeviceTimeError = (byte)0xc1;

        static final byte setUserPersonalInfo = 0x02;
        static final byte setUserPersonalInfoError = (byte)0x82;

        static final byte getUserPersonalInfo  = (byte)0x42;
        static final byte getUserPersonalInfoError  = (byte)0xC2;

        static final byte getDetailedCurrentDayActivityData = 0x43;
        static final byte getDetailedCurrentDayActivityDataResponse = 67;
        static final byte getDetailedCurrentDayActivityDataResponseByte = (byte)0x43;
        static final byte getDetailedCurrentDayActivityDataError = (byte)0xC3;

        static final byte deleteActivityData = 0x04;
        static final byte deleteActivityDataError = (byte)0x84;

        static final byte setDeviceIDCode = 0x05;
        static final byte setDeviceIDCodeError = (byte)0x85;

        static final byte queryDataStorageDistribution = 0x46;
        static final byte queryDataStorageDistributionError = (byte)0xC6;

        static final byte readSomedayTotalActivityData = 0x07;
        static final byte readSomedayTotalActivityDataError = (byte)0x87;

        static final byte readSomedayActivityGoalAchievedRate = 0x08;
        static final byte readSomedayActivityGoalAchievedRateError = (byte)0x88;

        static final byte startUpStepRealtimeMeterMode = 0x09;
        static final byte startUpStepRealtimeMeterModeError = (byte)0x89;

        static final byte stopStepRealtimeMeterMode = 0x0A;
        static final byte stopStepRealtimeMeterError = (byte)0x8A;

        static final byte setTargetSteps = 0x0B;
        static final byte setTargetStepsError = (byte)0x8B;

        static final byte getTargetSteps = 0x4B;
        static final byte getTargetStepsError = (byte)0xCB;

        static final byte setDistanceUnit = 0x0F;
        static final byte setDistanceUnitError = (byte)0x8F;

        static final byte getDistanceUnit = 0x4F;
        static final byte getDistanceUnitError = (byte)0xCF;

        static final byte getDevicesBatteryStatus = 0x13;
        static final byte getDevicesBatteryStatusError = (byte)0x93;

        static final byte getSoftwareVersion = 0x27;
        static final byte getSoftwareVersionError = (byte)0xA7;

        static final byte activateVibration = 0x36;
        static final byte activateVibrationResponse = 0x54;
        static final byte activateVibrationError = (byte)0xB6;

        static final byte setTimeFormat = 0x37;
        static final byte setTimeFormatError = (byte)0xB7;

        static final byte getTimeFormat = 0x38;
        static final byte getTimeFormatError = (byte)0xB8;

        static final byte setDeviceName = 0x3D;
        static final byte setDeviceNameError = (byte)0xBD;

        static final byte getDeviceName = 0x3E;
        static final byte getDeviceNameError = (byte)0xBE;

        static final byte showMessage = 0x51;
        static final byte showMessageError = (byte)0xD1;

        static final byte setMode = 0x49;
        static final byte setModeError = (byte)0xC9;

        static final byte getMode = 0x4A;
        static final byte getModeError = (byte)0xCA;

    //=============== Scales ==============
        static final byte getTestingDataFormat = 0x00;

        static final byte getSettings = (byte)0xfb;

        static final byte getUserProfiles = (byte)0xfd;

        static final byte weightWithResistor = (byte)0xFC;

        static final byte measurementResponse = (byte)0xff;

    //=============== Other ==============
        static final String trackerCharacteristicReadUuid = "fff7";    
        static final String trackerCharacteristicWriteUuid = "fff6";
        static final String trackerServiceUuid = "fff0";
    }

    public static byte calcCRC(byte[] message) {
        int crcSum = 0x00;
        for (int i = 0; i < 15; i++) {
          crcSum += message[i];
        }
        return (byte) (crcSum > 0xFF ? (byte) (crcSum % 256) & 0xFF : (byte) (crcSum) & 0xFF);
    }


}

