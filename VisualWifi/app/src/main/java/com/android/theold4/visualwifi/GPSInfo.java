package com.android.theold4.visualwifi;

/**
 * Created by park on 2015-11-23.
 */
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Jang on 2015-11-19.
 */
public class GPSInfo extends Service implements LocationListener{

    private LocationManager mLocMan;
    private boolean isGpsReceived;
    private Location mLoc;
    private String provider, provider2; // 프로바이더 이름
    private double lat = 38, lon = 127; // 기준 위/경도
    private final Context mContext;

    public GPSInfo(Context context) {
        this.mContext = context;
        onCreate();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isGpsReceived = false;
        mLocMan = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

        try {
            // -------------- NETWORK PROVIDER ------------------------
            provider = LocationManager.NETWORK_PROVIDER;

            if (provider != null){
                mLocMan.requestLocationUpdates(provider, 1000, 10, this);
            }
            // -------------- NETWORK PROVIDER ------------------------

            // ------------------ GPS PROVIDER ------------------------
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setSpeedRequired(true);
            criteria.setAltitudeRequired(true);
            provider2 = mLocMan.getBestProvider(criteria, true);

            if(provider2 != null){
                mLocMan.requestLocationUpdates(provider2, 1000, 10, this);
            }
            // ------------------ GPS PROVIDER ------------------------

            // 위치 조정
            if(mLoc != null){
                mLoc = mLocMan.getLastKnownLocation(provider);
                lat = mLoc.getLatitude();
                lon = mLoc.getLongitude();
            }
        } catch (SecurityException e) {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mLocMan.removeUpdates(this);
            mLocMan.removeUpdates(this);
        } catch (SecurityException e) {

        }
    }

    public double getLat(){
        return lat;
    }

    public double getLon(){
        return lon;
    }

    // 새로운 위치로 변경
    public void updateWithNewLocation(Location location, String provider) {
        // 여기에서 처리를 해준다.
        // provider 값으로 location이 어떤 provider에서 들어왔는지 알 수 있다.
        Location loc; // 최종적으로 확정된 위치정보가 저장 될 객체
        if (isGpsReceived) { // gps 수신여부 체크
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                mLoc = location; // gps 위치정보
                lat = mLoc.getLatitude();
                lon = mLoc.getLongitude();
            } else {
                try {
                    long gpsGenTime = mLocMan.getLastKnownLocation(LocationManager.GPS_PROVIDER).getTime(); // 마지막으로 수신된 GPS 위치정보
                    long curTime = System.currentTimeMillis(); // 현재 시간
                    if ((curTime - gpsGenTime) > 20000) { // gps 정보가 20초 이상 오래된 정보이면 네트워크 위치정보 사용
                        mLoc = location;
                        isGpsReceived = false; // 플래그를 해제
                    }
                } catch (SecurityException e) {

                }
            }
        } else {
            mLoc = location; // 네트워크 위치정보
            lat = mLoc.getLatitude();
            lon = mLoc.getLongitude();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if(isGpsReceived){
            isGpsReceived = true; // gps 위치정보가 수신되면 플래그를 set
            updateWithNewLocation(location, provider2);
        }else{
            updateWithNewLocation(location, provider);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}