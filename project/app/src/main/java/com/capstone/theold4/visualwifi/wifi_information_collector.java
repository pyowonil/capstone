package com.capstone.theold4.visualwifi;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by pyowo on 2015-12-03.
 */
public class wifi_information_collector extends Service implements LocationListener {
    // - - - - - - - - - - 와이파이 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    private WifiManager mWifiManager;
    private List<ScanResult> mScanResult;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                // TODO 와이파이 스캔 결과 얻기
                mWifiManager.startScan();
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                // TODO 와이파이 ... 이거 필요한가?
            }
        }
    };
    // - - - - - - - - - - 데이터베이스 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    private database_manager mDatabaseManager;
    private SQLiteDatabase mDatabaseWrite;
    // - - - - - - - - - - 위치정보 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    private LocationManager mLocationManager;
    private String mNetworkProvider;
    private String mGPSProvider;
    // - - - - - - - - - - 파라미터 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    private Location mLocation;

    // = = = = = = = = = = 서비스 쓰레드 = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    private Thread mThread;
    private Runnable mRun = new Runnable() {
        @Override
        public void run() {
            try {
                Log.i("[COLLECTOR]", "Start Service" + mLocation.toString());
                // 위치 저장
                double lat = Math.round(mLocation.getLatitude() * 1000d) / 1000d;
                double lng = Math.round(mLocation.getLongitude() * 1000d) / 1000d;

                // 시간 저장
                Date now = new Date(System.currentTimeMillis());
                int date = Integer.parseInt((new SimpleDateFormat("yyyyMMdd")).format(now));
                int time = Integer.parseInt((new SimpleDateFormat("HHmmss")).format(now));

                // 와이파이 정보
                mScanResult = mWifiManager.getScanResults();
                for (ScanResult scanresult : mScanResult) {
                    try {
                        String MAC = scanresult.BSSID;
                        String ssid = scanresult.SSID;
                        int rssi = scanresult.level;
                        //String capability = scanresult.capabilities;
                        String pw = "";

                        String head_query1 = "REPLACE INTO LocalDevice VALUES ('";
                        String head_query2 = "REPLACE INTO LocalData VALUES ('";
                        String tail_query1 = MAC + "', '" + lat + "', '" + lng + "', '" + ssid +
                                "', '" + pw + "', '" + date + "', '" + time + "');";
                        String tail_query2 = MAC + "', '" + lat + "', '" + lng + "', '" + ssid +
                                "', '" + rssi + "', '" + date + "', '" + time + "');";

                        mDatabaseWrite.execSQL(head_query1 + tail_query1);
                        mDatabaseWrite.execSQL(head_query2 + tail_query2);
                    } catch (SQLiteException e) {
                        // TODO 데이터베이스 에러처리 필요
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // TODO 종료전 마지막 작업
            }
        }
    };
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 서비스 쓰레드 = = = = = = = = = =

    // = = = = = = = = = = 액티비티 시작 (onCreate) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    public void onCreate() {
        super.onCreate();
        // 와이파이 관련 변수들
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        mWifiManager.startScan();
        // 데이터베이스 관련 변수들
        mDatabaseManager = new database_manager(this);
        try {
            mDatabaseWrite = mDatabaseManager.getWritableDatabase();
        } catch (SQLiteException e) {
            // TODO 데이터베이스 에러처리 필요
        }
        // 위치정보 관련 변수들
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            // - - - - - - - - - - Network provider - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            mNetworkProvider = LocationManager.NETWORK_PROVIDER;
            if (mNetworkProvider != null) {
                mLocationManager.requestLocationUpdates(mNetworkProvider, 1000, 10, this);
            }
            // - - - - - - - - - - GPS provider - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setSpeedRequired(true);
            criteria.setAltitudeRequired(true);
            mGPSProvider = mLocationManager.getBestProvider(criteria, true);
            if (mGPSProvider != null) {
                mLocationManager.requestLocationUpdates(mGPSProvider, 1000, 10, this);
            }
        } catch (SecurityException e) {
            // TODO 예외 처리 필요
        }

    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 액티비티 시작 (onCreate) = = = = = = = = = =

    // = = = = = = = = = = 서비스 시작 (onStartCommand) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // < START_STICKY >
        // 메모리부족이나 기타 상황에서 시스템이 강제로 service를 종료된후
        // service가 재시작될때 null Intent가 담긴 onStartCommand() 콜백함수가 실행된다.
        // 이 경우 null Intent로 호출때의 경우를 처리해줘야 합니다
        // < START_NOT_STICKY >
        // 이 경우는 프로세스가 강제로 종료되었을 경우 재시작하지 않고 종료된 상태로 남게 됩니다.
        // 예를 들면 매 15분마다 네트워크 체크를 하는 service가 강제로 종료되었을경우
        // 15분후에 자동적으로 다시 service가 실행되므로 재시작하지 않아도 되는 경우입니다.
        // < START_REDELIVER_INTENT >
        // 이 경우에는 프로세스가 강제로 종료되었을 경우 Intent가 다시 전달되어 재시작합니다.
        // 단, 여러차레 시도후 작업이 종료되지 않으면 service는 재시작 되지 않습니다.
        // 반드시 실행되어야 하는 service에 해당이 됩니다.
        return START_STICKY;
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 서비스 시작 (onStartCommand) = = = = = = = = = =

    // = = = = = = = = = = 서비스 종료 (onDestroy) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mLocationManager.removeUpdates(this);
            mLocationManager.removeUpdates(this);
        } catch (SecurityException e) {
            // TODO 예외처리 필요
        }
        unregisterReceiver(mReceiver);
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 서비스 종료 (onDestroy) = = = = = = = = = =

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            Log.i("[LOCATIONCHANGED]", "start thread");
            mLocation = location;
            mThread = new Thread(mRun);
            mThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // TODO 종료전 마지막 작업
            try {
                mThread.join();
                Log.i("[COLLECTOR]", "Finish Service");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i("[LOCATIONCHANGED]", "end thread");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO provider 변경 처리
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO provider 변경 처리
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO provider 변경 처리
    }
}
