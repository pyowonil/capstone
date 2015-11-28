package com.android.theold4.visualwifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.lang.Comparable;
import java.util.Comparator;
/**
 * Created by Jang on 2015-11-22.
 * 자동연결을 위한 서비스
 */
public class AutoConnectService extends Service implements Runnable {

    public static final String TAG = "Auto";
    public int count =0;
    Thread myThread;



    public void onCreate(){
        super.onCreate();

        Log.i(TAG,"Autostart");

        Log.i(TAG, "Autorun()");
    }

    public void onDestroy(){
        myThread.interrupt();
    }

    //run 함수안에서 while 로 스캔 - 정렬 - open / DB검사 -  sleep2초 주기로 반복
   public void run(){

    }

    public IBinder onBind(Intent arg0){
        return null;
    }
}
