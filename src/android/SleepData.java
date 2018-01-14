package com.megster.cordova.ble.central;

import org.apache.cordova.LOG;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SleepData {
    String time;
    int index;
    public int restfulness ;

    public SleepData(byte[] data){
        time =  "20" ;
        int year = Integer.valueOf(Integer.toString(data[2],16));
        int month = Integer.valueOf(Integer.toString(data[3],16));
        int day = Integer.valueOf(Integer.toString(data[4],16));
        time += year < 10? "0" : "" + String.valueOf(year)+ "-";
        time += month < 10? "0" : "";
        time += String.valueOf(month) + "-";
        time += day < 10? "0" : "";
        time += String.valueOf(day);

        Date now = new Date();
        time += "T" + new SimpleDateFormat("hh:mm:ss")
                .format(new Date( data[5]*15*60000
                       - TimeZone.getDefault().getOffset(now.getTime())));
        index = data[5];
        if( data[6] != 0x00 ){
            restfulness = 0;
            int restfulCount = 0;
            for(int i=0; i<8; i++){
                if(data[i+7]!=0) {
                    restfulCount++;
                    int sleepData = (data[i + 7] < 0 ?  128 : data[i + 7]);
                    restfulness += 100.0 - ((sleepData) / 1.28);
                }
            }
            restfulness = restfulCount != 0 ? restfulness/restfulCount : 0;
        }
    }

    public void print(){
        LOG.d("SleepData", "Time: (" + time
                + ") restfulness : (" + String.valueOf(restfulness)+")");
    }
}
