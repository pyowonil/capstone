package com.example.edge.myapplication;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by edge on 2015-11-02.
 * http://mainia.tistory.com/1153
 */
public class GpsInfo extends Service implements LocationListener {

    private final Context mContext;

    // 현재 GPS 사용유무
    boolean isGPSEnabled = false;

    // 네트워크 사용유무
    boolean isNetworkEnabled = false;

    // GPS 상태값
    boolean isGetLocation = false;

    Location location;
    double lat; // 위도
    double lon; // 경도

    // 최소 GPS 정보 업데이트 거리 10미터
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;

    // 최소 GPS 정보 업데이트 시간 밀리세컨이므로 1분
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;

    protected LocationManager locationManager;

    public GpsInfo(Context context) {
        this.mContext = context;
        getLocation();
    }

    Criteria criteria = new Criteria();
    LocationListener locationListener;


    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // GPS 정보 가져오기
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // 현재 네트워크 상태 값 알아오기
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);




            if (!isGPSEnabled && !isNetworkEnabled) {
                // GPS 와 네트워크사용이 가능하지 않을때 소스 구현
            } else {
                this.isGetLocation = true;
                // 네트워크 정보로 부터 위치값 가져오기
                if (isNetworkEnabled) {

                    criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                    criteria.setSpeedRequired(true);
                    criteria.setAltitudeRequired(true);
                    criteria.setBearingRequired(true);
                    criteria.setCostAllowed(true);
                    criteria.setPowerRequirement(Criteria.POWER_LOW);

//                    locationManager.requestLocationUpdates(
//                            LocationManager.NETWORK_PROVIDER,
//                            MIN_TIME_BW_UPDATES,
//                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria,true),1000,0,this);

                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(locationManager.getBestProvider(criteria,true));
//                        location = locationManager
//                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            // 위도 경도 저장
//                            lat = location.getLatitude();
//                            lon = location.getLongitude();
                            onLocationChanged(location);
                        }
                    }
                }

                if (isGPSEnabled) {
                    if (location == null) {
                        criteria.setAccuracy(Criteria.ACCURACY_FINE);
                        criteria.setSpeedRequired(true);
                        criteria.setAltitudeRequired(true);
                        criteria.setBearingRequired(true);
                        criteria.setCostAllowed(false);
                        criteria.setPowerRequirement(Criteria.POWER_LOW);

                        locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria,true),1000,0,this);

//                        locationManager.requestLocationUpdates(
//                                LocationManager.GPS_PROVIDER,
//                                MIN_TIME_BW_UPDATES,
//                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(locationManager.getBestProvider(criteria,true));
//                            location = locationManager
//                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                lat = location.getLatitude();
                                lon = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    /**
     * GPS 종료
     * */
    public void stopUsingGPS(){
        if(locationManager != null){
            //locationManager.removeUpdates(GpsInfo.this);
        }
    }

    /**
     * 위도값을 가져옵니다.
     * */
    public double getLatitude(){
        if(location != null){
            lat = location.getLatitude();
        }
        return lat;
    }

    /**
     * 경도값을 가져옵니다.
     * */
    public double getLongitude(){
        if(location != null){
            lon = location.getLongitude();
        }
        return lon;
    }

    public double getAltitude(){
        return location.getAltitude();
    }

    public double getSpeed(){
        return location.getSpeed();
    }

    public double getBearing(){
        return location.getBearing();
    }

    /**
     * GPS 나 wife 정보가 켜져있는지 확인합니다.
     * */
    public boolean isGetLocation() {
        return this.isGetLocation;
    }

    /**
     * GPS 정보를 가져오지 못했을때
     * 설정값으로 갈지 물어보는 alert 창
     * */
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        alertDialog.setTitle("GPS 사용유무셋팅");
        alertDialog.setMessage("GPS 셋팅이 되지 않았을수도 있습니다.\n 설정창으로 가시겠습니까?");

        // OK 를 누르게 되면 설정창으로 이동합니다.
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        mContext.startActivity(intent);
                    }
                });
        // Cancle 하면 종료 합니다.
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public float bearing;
    public long time;
    public float altitude;
    public float speed;
    Location before = null;
    public float distance;

    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        Log.d("[위치] ", "갱신1!");
        try{
            if(before == null){
                before = location;
            }
            location = locationManager
                    .getLastKnownLocation(locationManager.getBestProvider(criteria,true));

            bearing = location.bearingTo(location);

            distance = location.distanceTo(location);
            lat = location.getLatitude();
            lon = location.getLongitude();
//            bearing = location.getBearing();
            time = location.getTime();

        }catch(SecurityException e){

        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
        Log.d("[위치] ", "갱신2!");
        try{
            location = locationManager
                    .getLastKnownLocation(locationManager.getBestProvider(criteria,true));
        }catch(SecurityException e){

        }
    }

    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        Log.d("[위치] ", "갱신3!");
        try{
            location = locationManager
                    .getLastKnownLocation(locationManager.getBestProvider(criteria,true));
        }catch(SecurityException e){

        }
    }

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
        Log.d("[위치] ", "갱신4!");
        try{
            location = locationManager
                    .getLastKnownLocation(locationManager.getBestProvider(criteria,true));
        }catch(SecurityException e){

        }
    }
}
