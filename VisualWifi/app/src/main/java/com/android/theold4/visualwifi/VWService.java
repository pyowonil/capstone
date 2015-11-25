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

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
    private double lat = 38;
    private double lon = 127;

    TelephonyManager telephonyManager;
    GsmCellLocation cellLocation;
    WifiManager wifiManager;
    List<ScanResult> apList;

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
                    double distance = Math.pow(10, (info.getRssi() - 32.44 - 20 * Math.log10(info.getFrequency())) / 20);
                    String str = "SSID : " + info.getSSID() +
                            "\nMAC ADDR : " + info.getMacAddress() +
                            //"\tFREQ : " + info.getFrequency() +
                            "\nLINK SPD : " + info.getLinkSpeed() +
                            "\nLP ADDR : " + info.getIpAddress() +
                            "\nNET ID : " + info.getNetworkId() +
                            "\nRSSI : " + info.getRssi() +
                            "\nDISTANCE : " + distance*1000;
                    Log.d("[연결된 와이파이 AP] ", str);

                    // FSPL(db) = 20log10(d) + 20log10(f) + K
                    // d = distance
                    // f = frequency
                    // K = constant that depends on the units used for d and f
                    // db - 32.44 - 20log10(f) / 20


                    List<ScanResult> apList = wifimanager.getScanResults();

//                    for(ScanResult result : apList){
//                        int level = WifiManager.calculateSignalLevel(info.getRssi(), result.level);
//                        int difference = (level*100)/result.level;
//                        String ssid = result.SSID;
//                        int level2 = result.level;
//                        int freq = result.frequency;
//                        int freq2 = result.centerFreq0;
//                        int freq3 = result.centerFreq1;
//                        int chw = result.channelWidth;
//                        long time = result.timestamp;
//                        String str2 = "SSID : " + ssid +
//                                "\nLEVEL : " + level2 +
//                                "\nFREQ : " + freq +
//                                "\nCHANNEL WIDTH : " + chw+
//                                "\nCENTER FREQ1 : " + freq2 +
//                                "\nCENTER FREQ1 : " + freq3 +
//                                "\nTIMESTAMP : " + time;
//                        Log.d("[와이파이 AP] ", str2);
//                    }
//
//                    String ssid = info.getSSID();
//                    int rssi = info.getRssi();
//
//
//                    int cellid= cellLocation.getCid();
//                    int celllac = cellLocation.getLac();
//                    Log.d("CellLocation", cellLocation.toString());
//                    Log.d("GSM CELL ID", String.valueOf(cellid));
//                    Log.d("GSM Location Code", String.valueOf(celllac));

                    lat = gps.getLat();
                    lon = gps.getLon();

//                    db.execSQL("INSERT INTO contact VALUES(null, '" + ssid + "','" + rssi + "');");

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