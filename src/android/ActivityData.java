package com.megster.cordova.ble.central;

// import org.apache.cordova.LOG;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ActivityData extends Object {
  String time;
  int index;
  float calories;
  int steps;
  float distance;

  public ActivityData(byte[] data){
    //   LOG.d("ActivityData " + data);
      time =  "20" ;
      int year = Integer.valueOf(Integer.toString(data[2],16));
      int month = Integer.valueOf(Integer.toString(data[3],16));
      int day = Integer.valueOf(Integer.toString(data[4],16));
      time += year < 10? "0" : "" + String.valueOf(year)+ "-";
      time += month < 10? "0" : "";
      time += String.valueOf(month) + "-";
      time += day < 10? "0" : "";
      time += String.valueOf(day);
      index = data[5];
      Date now = new Date();
      time += "T" + new SimpleDateFormat("hh:mm:ss")
              .format(new Date( data[5]*15*60000 -
                      TimeZone.getDefault().getOffset(now.getTime())));
      if( data[6] == 0x00 ){
          calories = (float)( data[8] < 0 ? data[8] & 0xff : data[8]);
          calories *= 256;
          calories += (float)( data[7] < 0 ? data[7] & 0xff : data[7]);
          calories = calories / 100;

          steps = data[10] < 0 ? data[10] & 0xff : data[10];
          steps *= 256;
          steps += data[9] < 0 ? data[9] & 0xff : data[9];

          distance = (float)( data[12] < 0 ? data[12] & 0xff : data[12]);
          distance *= 256;
          distance += (float)( data[11] < 0 ? data[11] & 0xff : data[11]);
      }
  }

  public void print(){
        // Log.d("ActivityData", "Time: (" + time
        //       + ") calories: (" + String.valueOf(calories)
        //       + ") steps: (" + String.valueOf(steps)
        //       + ") distance: (" + String.valueOf(distance)+")");
  }

  public String getTime() {
      return time;
  }

  public int getIndex() {
      return index;
  }

  public float getCalories() {
      return calories;
  }

  public int getSteps() {
      return steps;
  }

  public float getDistance() {
      return distance;
  }

}