package com.android.theold4.visualwifi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

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
        // 쓰레드 객체 생성 후 시작
        myThread = new Thread(this);
        myThread.start();
    }

    public void onDestroy(){
        myThread.interrupt();
    }

    public void run(){
        Log.i(TAG,"Autorun()");
        // 실행부 ( 예시 로그에 5초마다 카운트 )
        // 자동연결 하는 코드
        while(true){
            try{
                Log.i(TAG, "자동연결 서비스 #" + count);
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
