package com.android.theold4.visualwifi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Jang on 2015-11-21.
 * 목적 : VisualWifi의 서비스 ( Wifi 데이터 수집 )
 */
public class VWService extends Service implements Runnable {

    public static final String TAG = "MapsActivity.java";
    public int count =0;
    Thread myThread;

    public void onCreate(){
        super.onCreate();

        // 쓰레드 객체 생성 후 시작
        myThread = new Thread(this);
        Log.i("rec","thread");
        myThread.start();
    }

    public void onDestroy(){
        myThread.interrupt();
    }

    public void run(){
        // 실행부 ( 예시 로그에 5초마다 카운트 )
        while(true){
            try{
                Log.i(TAG, "my service called #" + count);
                count++;

                Thread.sleep(5000);
            }catch(InterruptedException ex){
                Log.e(TAG, ex.toString());
                break;
            }
        }
    }
    public IBinder onBind(Intent arg0){
        return null;
    }
}