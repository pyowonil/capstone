package com.android.theold4.visualwifi;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by Jang on 2015-11-21.
 * 목적 : 서비스가 Wifi로 연결되어있을 때만 동작하고 꺼진상태에서는 동작하지 않게 하는 BroadcastReceiver
 */
public class NetWatcher extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        Log.i("rec", "onRecive start");
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        Intent intentService = new Intent(context,VWService.class);
        if(info!=null && info.getType()==ConnectivityManager.TYPE_WIFI){
            //start service
            Log.i("rec", "WIFI connect");
            context.startService(intentService);
        }
        else{
            Log.i("rec", "WiFI End");
            //stop service
            context.stopService(intentService);
        }

    }
   /* add in Manifest
   <receiver android:name=" %packagename% .NetWatcher">
     <intent-filter>
       <action android:name="android.net.conn.CONNECTIVITY_CHANGE"/>
     </intent-filter>
   </receiver>
    */
}