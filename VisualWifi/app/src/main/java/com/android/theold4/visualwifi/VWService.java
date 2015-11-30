package com.android.theold4.visualwifi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.lang.Integer;

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

    GPSInfo gps;


    TelephonyManager telephonyManager;
    GsmCellLocation cellLocation;
    WifiManager wifiManager;
    List<ScanResult> apList;

    private String Mac;
    String ssid;
    String pw;
    int rssi;
    private double lat = 38;
    private double lon = 127;
    Date MyDate;


    public void onCreate(){
        super.onCreate();

        gps = new GPSInfo(this);
        telephonyManager = (TelephonyManager)getSystemService(this.TELEPHONY_SERVICE);
        cellLocation = (GsmCellLocation)telephonyManager.getCellLocation();

        // 쓰레드 객체 생성 후 시작
        myThread = new Thread(this);
        Log.i(TAG,"thread");
        myThread.start();
    }

    public void onDestroy(){
        myThread.interrupt();
    }

    public void run(){
        Integer date;
        Integer time;

        helper = new DBManager(this);
        try {
            db = helper.getWritableDatabase();
            //데이터베이스 객체를 얻기 위하여 getWritableDatabse()를 호출
        } catch (SQLiteException e) {
            db = helper.getReadableDatabase();
        }


        while(true){
            try{
                if(getNetworkInfo() == 1) {

                    // 시간 저장
                    long now = System.currentTimeMillis();
                    MyDate = new Date(now);
                    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");
                    SimpleDateFormat sdfTime = new SimpleDateFormat("HHmmss");
                    String strDate = sdfDate.format(MyDate);
                    String strTime = sdfTime.format(MyDate);

                    date =  Integer.getInteger(strDate);
                    time =  Integer.getInteger(strTime);

                    // Wifi 정보 저장
                    WifiManager wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

                    WifiInfo info = wifimanager.getConnectionInfo();  // 연결된 와이파이 정보

                    Mac = info.getMacAddress();
                    ssid = info.getSSID();
                    rssi = info.getRssi();
                    pw = "1";

                    lat = gps.getLat();
                    lon = gps.getLon();

                    // Local DB에 insert
                    db.execSQL("INSERT INTO LocalDevice VALUES('"
                            +Mac+"', '"
                            +lat+"','"
                            +lon+"','"
                            +ssid+"','"
                            +pw+"','"
                            +date+"','"
                            +time+"');"
                    );


                    db.execSQL("INSERT INTO LocalData VALUES('"
                            +Mac+"','"
                            +lat+"','"
                            +lon+"','"
                            +ssid+"','"
                            +rssi+"','"
                            +date+"','"
                            +time+ "');"
                    );




                }
                String sql = "SELECT * FROM LocalData;";
                Cursor c = db.rawQuery(sql, null);
                c.moveToFirst();

                while(c.moveToNext()){
                    String _mac = c.getString( c.getColumnIndex("Mac"));
                    float _lat = c.getFloat(c.getColumnIndex("Latitude"));
                    float _lng = c.getFloat(c.getColumnIndex("Longitude"));
                    String _ssid = c.getString(c.getColumnIndex("ssid"));
                    int _rssi = c.getInt(c.getColumnIndex("rssi"));
                    int _date = c.getInt(c.getColumnIndex("Date"));
                    int _time = c.getInt(c.getColumnIndex("Time"));


                    Log.i("LocalData", "mac : "+ _mac +"  //  id : "+ _ssid+" ,(lat,lng) : "+ _lat + ","+_lng +"");

                }
                Log.i("LocalData","------------------------------------------");
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