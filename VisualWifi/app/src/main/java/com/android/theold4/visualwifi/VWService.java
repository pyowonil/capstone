package com.android.theold4.visualwifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.Collections;
import java.util.Comparator;
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

        wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        initWIFIScan();
        if (wifiManager.isWifiEnabled() == false)
            wifiManager.setWifiEnabled(true);

        // 쓰레드 객체 생성 후 시작
        myThread = new Thread(this);
        myThread.start();
    }

    private List<ScanResult> mScanResult; // ScanResult List

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                getWIFIScanResult(); // get WIFISCanResult
                wifiManager.startScan(); // for refresh
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
            }
        }
    };

    class ScanResultComparator implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
            int level_of_o1 = (((ScanResult)o1).level);
            int level_of_o2 = (((ScanResult)o2).level);
            return level_of_o2 - level_of_o1;
        }
    }

    public void getWIFIScanResult() {

        mScanResult = wifiManager.getScanResults(); // ScanResult
        // 정렬한 후 출력하면 제대로 정렬이 되는지 확인
        Collections.sort(mScanResult, new ScanResultComparator());
    }

    public void initWIFIScan() {
        // init WIFISCAN
        final IntentFilter filter = new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        wifiManager.startScan();
        Log.d(TAG, "initWIFIScan()");
    }

    public void onDestroy(){
        myThread.interrupt();
    }

    public void run(){
        Integer date;
        Integer time;
        int _date;
        int _time;

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

                    _date = Integer.parseInt(strDate);
                    _time = Integer.parseInt(strTime);


                    // Wifi 정보 저장
                    WifiManager wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

                    WifiInfo info = wifimanager.getConnectionInfo();  // 연결된 와이파이 정보

                    getWIFIScanResult();
                    lat = gps.getLat();
                    lon = gps.getLon();
//                    lat = Math.round(lat * 1000d) / 1000d;   // 본래 도 에서 소수점 6자리 , 주석 풀면 3자리
//                    lon = Math.round(lon * 1000d) / 1000d;
                    for(ScanResult scanresult : mScanResult) {
                        Mac = scanresult.BSSID;
                        ssid = scanresult.SSID;
                        rssi = scanresult.level;

                        pw = "NULL";

                        try {
                            // Local DB에 insert
                            db.execSQL("REPLACE INTO LocalDevice VALUES ('"
                                            + Mac + "', '"
                                            + lat + "', '"
                                            + lon + "', '"
                                            + ssid + "', '"
                                            + pw + "', '"
                                            + _date + "', '"
                                            + _time + "');"
                            );

                            db.execSQL("REPLACE INTO LocalData VALUES ('"
                                            + Mac + "', '"
                                            + lat + "', '"
                                            + lon + "', '"
                                            + ssid + "', '"
                                            + rssi + "', '"
                                            + _date + "', '"
                                            + _time + "');"
                            );
                            // rssi  // ~ -25 , 26~40 , 41~55, 56~70, 71~85
                        } catch (Exception e) {
                            Log.i("DB", "insert error");
                            e.printStackTrace();
                        }
                    }
                }
                Thread.sleep(1000);

                // 쿼리로 DB내용 확인
                String sql = "SELECT * FROM LocalDevice;";
                Cursor c = db.rawQuery(sql, null);
                while(c.moveToNext()){
                    String _mac = c.getString(c.getColumnIndex("MAC"));
                    float _lat = c.getFloat(c.getColumnIndex("Latitude"));
                    float _lng = c.getFloat(c.getColumnIndex("Longitude"));
                    String _ssid = c.getString(c.getColumnIndex("SSID"));
                    String _pw = c.getString(c.getColumnIndex("PW"));
                    //int _rssi = c.getInt(c.getColumnIndex("RSSI"));
                    _date = c.getInt(c.getColumnIndex("DATE"));
                    _time = c.getInt(c.getColumnIndex("TIME"));

                    //_date -> yyyymmdd 형식   _time -> hhmmss  mmss 는 4자리 고정, h만 1~2자리
                   // Log.i("LocalData", "mac : "+ _mac +"  //  id : "+ _ssid+" ,(lat,lng) : "+ _lat + ","+_lng +"");
                   // Log.i("LocalData", "ssid : "+ _ssid+"   // rssi : "+_pw+"   date : "+_date+" "+_time);
                  // Log.i("LocalData", "ssid : "+ _ssid+"   // rssi : "+_rssi+"   date : "+_date+" "+_time);

                }
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