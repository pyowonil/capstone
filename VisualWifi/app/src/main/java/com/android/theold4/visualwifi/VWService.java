package com.android.theold4.visualwifi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Jang on 2015-11-21.
 * 목적 : VisualWifi의 서비스 ( Wifi 데이터 수집 )
 */
public class VWService extends Service implements Runnable {

    public static final String TAG = "VWS";
    public int count =0;
    Thread myThread;

    DBManager helper;
    SQLiteDatabase db;

    public void onCreate(){
        super.onCreate();

        // 쓰레드 객체 생성 후 시작
        myThread = new Thread(this);
        Log.i(TAG,"thread");
        myThread.start();
    }

    public void onDestroy(){
        myThread.interrupt();
    }

    public void run(){
        // 실행부 ( 예시 로그에 5초마다 카운트 )
        Log.i("ABC", "helper0");
        helper = new DBManager(this);
        Log.i("ABC", "helper");
        try {
            db = helper.getWritableDatabase();
            Log.i("ABC", "getwritabledb");
            //데이터베이스 객체를 얻기 위하여 getWritableDatabse()를 호출

        } catch (SQLiteException e) {
            db = helper.getReadableDatabase();
            Log.i("ABC", "getReadabledb");
        }

        while(true){
            try{
                if(getNetworkInfo() == 1) {
                    WifiManager wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

                    WifiInfo info = wifimanager.getConnectionInfo();

                    String ssid = info.getSSID();
                    int rssi = info.getRssi();

                    db.execSQL("INSERT INTO contact VALUES(null, '" + ssid + "','" + rssi + "');");

                }
                String sql = "SELECT * FROM contact;";
                Cursor c = db.rawQuery(sql, null);
                c.moveToFirst();

                while(c.moveToNext()){
                    int num = c.getInt(c.getColumnIndex("_id"));
                    String id = c.getString(c.getColumnIndex("ssid"));
                    String pw = c.getString(c.getColumnIndex("passwd"));

                    Log.i("cwifi", "num "+num+" / id : "+id+" ,pw :"+pw );
                }
                Log.i("cwifi","------------------------------------------");
                Thread.sleep(3000);
            }catch(InterruptedException ex){
                Log.e(TAG, ex.toString());
                break;
            }
        }
    }
    public int getNetworkInfo() {
        int result = 3;
        // Check Network State -->
        ConnectivityManager connectivityManager;
        NetworkInfo networkInfo;
        connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            result = 2;
        } else {
            if (networkInfo.getType() == 0) {
                result = 0;      // 3G MOBILE
            } else {
                result = 1;     // WIFI
            }
        }
        // <-- Check Network State
        return result;
    }

    public IBinder onBind(Intent arg0){
        return null;
    }
}