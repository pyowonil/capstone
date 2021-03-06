package com.capstone.theold4.visualwifi;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class visual_wifi_map extends AppCompatActivity
    implements
        OnMapReadyCallback, // Google Map 준비 (기본)
        GoogleMap.OnMyLocationButtonClickListener, // MyLocation 버튼
        GoogleMap.OnMyLocationChangeListener, // MyLocation Change 리스너
        GoogleMap.OnMapClickListener, // 맵 클릭 리스너(짧게)
        GoogleMap.OnMapLongClickListener, // 맵 클릭 리스너(길게)
        GoogleMap.OnMarkerClickListener, // 마커 클릭 리스너
        ActivityCompat.OnRequestPermissionsResultCallback // Permission Request Callback 기능
{
    // = = = = = = = = = = WIFI Connection = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    // - - - - - - - - - - WIFI AUTO SERVICE - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    private boolean mIsAutoConnection;
    private Intent mAutoConnectionServiceIntent;
    private ComponentName mAutoConnectionServiceName;
    // - - - - - - - - - - WIFI MANUAL OPERATION - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    private WifiManager mWifiManager;
    private List<ScanResult> mScanResult;
    private String mSelectedSSID, mSelectedCapability, mSelectedMAC;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                // TODO 와이파이 스캔 결과 얻기
                //mWifiManager.startScan();
            } else if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                // TODO 와이파이 ... 이거 필요한가?
            }
        }
    };

    // 정렬 알고리즘
    private class ScanResultComparator implements Comparator<ScanResult> {
        @Override
        public int compare(ScanResult lhs, ScanResult rhs) {
            return rhs.level - lhs.level;
        }
    }
    private Comparator<ScanResult> mScanResultComparator = new ScanResultComparator();
    private void alertWifiManualList() {
        mScanResult = mWifiManager.getScanResults();
        Collections.sort(mScanResult, mScanResultComparator);
        // TODO (!) 표시도 필요할듯
        final String[] scanResults;
        {
            final String[] scanResult = new String[mScanResult.size()];
            int size = mScanResult.size();
            int len = 0;

            for (int i = 0; i < size; i++) {
                ScanResult scanresult = mScanResult.get(i);
                if(scanresult.SSID.equals("")) continue;
                boolean isDuplication = false;
                for(int j = 0; j < i; j++) {
                    if(scanresult.SSID.equals(mScanResult.get(j).SSID)) {
                        isDuplication = true;
                        break;
                    }
                }
                if(!isDuplication) {
                    // TODO 강도 표현
                    scanResult[len] = scanresult.SSID;
                    len += 1;
                }
            }
            scanResults = new String[len];
            for(int i=0; i<len; i++) {
                scanResults[i] = scanResult[i];
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(visual_wifi_map.this);
        builder.setTitle(getResources().getString(R.string.wifi_manual_list_title));
        builder.setItems(scanResults, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSelectedSSID = scanResults[which];
                for (ScanResult scanresult : mScanResult) {
                    if (scanresult.SSID.equals(mSelectedSSID)) {
                        mSelectedCapability = scanresult.capabilities;
                        mSelectedMAC = scanresult.BSSID;
                        break;
                    }
                }
                dialog.dismiss();
                if (mSelectedCapability.contains("OPEN") || mSelectedCapability.equals("[ESS]")) {
                    Log.i("[WIFI_MANAUAL]", "try connect ssid=" + mSelectedSSID + " capability=" + mSelectedCapability);
                    connect(mSelectedSSID, "", mSelectedCapability);
                } else {
                    alertWifiLogin();
                }
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.wifi_manual_list_title_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
            }
        });
        builder.show();
    }
    private void alertWifiLogin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(visual_wifi_map.this);
        final View loginView = getLayoutInflater().inflate(R.layout.alert_wifi_login, null);
        builder.setView(loginView);
        builder.setTitle(getResources().getString(R.string.wifi_manual_login_title));
        builder.setPositiveButton(getResources().getString(R.string.wifi_manual_login_button_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText edit_password = (EditText) loginView.findViewById(R.id.password);
                String password = edit_password.getText().toString();
                // TODO 비밀번호 저장
                {
                    String query_isExist = "SELECT PW FROM WifiShare WHERE SSID = '" + mSelectedSSID + "';";
                    Cursor cursor = mDatabaseRead.rawQuery(query_isExist, null);

                    if(!cursor.moveToNext()) {
                        // Not Exist
                        // 시간 저장
                        Date now = new Date(System.currentTimeMillis());
                        int date = Integer.parseInt((new SimpleDateFormat("yyyyMMdd")).format(now));
                        int time = Integer.parseInt((new SimpleDateFormat("HHmmss")).format(now));
                        String query_insert = "REPLACE INTO WifiShare VALUES ('" + mSelectedMAC + "', '" +
                                mSelectedSSID + "', '" + password + "', '" + mSelectedCapability + "', '" +
                                date + "', '" + time + "');";
                        mDatabaseWrite.execSQL(query_insert);
                        Log.i("[WIFI_MANUAL]", "NOT EXIST - SAVE : " + mSelectedSSID + " " + password);
                    } else {
                        // Exist
                        String query_update = "UPDATE WifiShare SET PW = '" + password + "' WHERE MAC = '" +
                                mSelectedMAC + "';";
                        mDatabaseWrite.execSQL(query_update);
                        Log.i("[WIFI_MANUAL]", "EXIST - SAVE : " + mSelectedSSID + " " + password);
                    }

                    cursor = mDatabaseRead.rawQuery(query_isExist, null);
                    if(cursor.moveToNext()) {
                        Log.i("[WIFI_MANUAL_DATABASE]", cursor.getString(cursor.getColumnIndex("PW")));
                    } else {
                        Log.i("[WIFI_MANUAL_DATABASE]", "FAIL");
                    }
                }

                Log.i("[WIFI_MANUAL]", "try connect ssid=" + mSelectedSSID + " password=" + password + " capability=" + mSelectedCapability);
                connect(mSelectedSSID, password, mSelectedCapability);
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.wifi_manual_login_button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
    public boolean connect(String SSID, String password, String capability) {
        // TODO 자세한 메커니즘 확인 필요
        boolean success = false;
        WifiConfiguration wfc = new WifiConfiguration();

        wfc.SSID = "\"".concat( SSID ).concat("\"");
        wfc.status = WifiConfiguration.Status.ENABLED;
        wfc.priority = 40;

        if(capability.contains("WEP") == true ){
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wfc.wepKeys[0] = "\"".concat(password).concat("\"");
            wfc.wepTxKeyIndex = 0;
        }else if(capability.contains("WPA") == true ) {
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wfc.preSharedKey = "\"".concat(password).concat("\"");
        }else if(capability.contains("WPA2") == true ) {
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wfc.preSharedKey = "\"".concat(password).concat("\"");
        }else if(capability.contains("OPEN") == true || capability.contains("ESS")) {
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wfc.allowedAuthAlgorithms.clear();
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        }

        int networkId = mWifiManager.addNetwork(wfc);
        if(networkId != -1) {
            success = mWifiManager.enableNetwork(networkId, true);
        }

        return success;
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = WIFI Connection = = = = = = = = = =

    // = = = = = = = = = = WIFI 업로드 = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    private void alertWifiUpload() {
        // 데이터베이스에서 검색한 결과를 리스트에 출력
        ArrayList<String> items_ssid = new ArrayList<String>();
        ArrayList<String> items_mac = new ArrayList<String>();
        {
            String query_find = "SELECT * FROM WifiShare;";
            Cursor cursor = mDatabaseRead.rawQuery(query_find, null);
            int id_ssid = cursor.getColumnIndex("SSID");
            int id_mac = cursor.getColumnIndex("MAC");
            while(cursor.moveToNext()) {
                Log.i("[UPLOAD]", cursor.getString(id_ssid) + " " + cursor.getString(id_mac));
                items_ssid.add(cursor.getString(id_ssid));
                items_mac.add(cursor.getString(id_mac));
            }
        }
        final String[] items = new String[items_ssid.size()];
        final String[] macs = new String[items_mac.size()];
        items_ssid.toArray(items);
        items_mac.toArray(macs);

        AlertDialog.Builder builder = new AlertDialog.Builder(visual_wifi_map.this);
        builder.setTitle(getResources().getString(R.string.wifi_upload_title));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 데이터베이스에서 검색하여 모든 내용을 받아오기
                {
                    String query_find = "SELECT * FROM WifiShare WHERE MAC ='" + macs[which] + "';";
                    Cursor cursor = mDatabaseRead.rawQuery(query_find, null);
                    if(cursor.moveToNext()) {
                        UploadAsyncTask task = new UploadAsyncTask();
                        task.execute(macs[which]);
                        // 결과
                        // MAC : macs[which]
                        // SSID : items[which]
                        // PW : cursor.getString(cursor.getColumnIndex("PW"))
                        // ~~~
                        // TODO 업로드 서비스 시작
                        Log.i("[WIFI Upload]", items[which] + " " + macs[which] + " " + cursor.getString(cursor.getColumnIndex("PW")));
                    } else {
                        // 결과 없음 ... 에러
                        // TODO 에러 처리
                    }
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.wifi_upload_title_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = WIFI 업로드드 =  = = = = = = = =

    // = = = = = = = = = = WIFI Information Collector = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    private boolean mIsInformationCollection;
    private Intent mInformationCollectorIntent;
    private ComponentName mInformationCollectorName;
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = WIFI Information Collector = = = = = = = = = =

    // = = = = = = = = = = DATABASE MANAGER = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    private database_manager mDatabaseManager;
    private SQLiteDatabase mDatabaseWrite, mDatabaseRead;
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = DATABASE MANAGER = = = = = = = = = =

    // = = = = = = = = = = Permission Request Callback = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    // Request code for location permission request.
    // #onRequestPermissionResult(int, String[], int[])
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    // Flag indicating whether a requested permission has been denied after returning in
    // {#onRequestPermissionsResult(int, String[], int[])}
    private boolean mPermissionDenied = false;
    //@Override
    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }
    // Displays a dialog with error message explaining that the location permission is missing.
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog.newInstance(true).show(getSupportFragmentManager(), "dialog");
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = Permission Request Callback = = = = = = = = = =

    // = = = = = = = = = = Google Map API = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    private GoogleMap mMap;
    private float mInitLocation_latitude, mInitLocation_longitude;
    private float mInitLocation_zoom;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationChangeListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        enableMyLocation();
    }
    // Enables the My Location layer if the fine location permission has been granted.
    private void enableMyLocation() {
        // - - - - - - - - - - Permission Request Callback - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    // 신호 강도, AP 위치 그리는 리스트
    private List<SignalInfo> mSignalInfoList;
    private boolean checkFirstPos = false;
    private boolean checkComputeComplete = false;
    private LatLng prevPos = null;
    private float[] distance = new float[2];

    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        Location location = mMap.getMyLocation();
        if(location != null){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17));
            if(checkComputeComplete) {
                if(prevPos == null){
                    prevPos = new LatLng(location.getLatitude(), location.getLongitude());
                    drawSignal();
                }else{
                    Location.distanceBetween(prevPos.latitude,prevPos.longitude,
                            location.getLatitude(),location.getLongitude(),distance);
                    if(distance[0] >= 5) {
                        prevPos = new LatLng(location.getLatitude(), location.getLongitude());
                        drawSignal();
                    }
                }

            }
        }
        return false;
    }

    private APPointAsyncTask task;

    @Override
    public void onMyLocationChange(Location location) {
        // TODO 위치 변경 이벤트 처리
        if(location != null){
            if(!checkFirstPos){
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 18));
                checkFirstPos = true;

                task = new APPointAsyncTask();
                task.execute(new LatLng(location.getLatitude(), location.getLongitude()));
            }
            if(checkComputeComplete) {
                if(prevPos == null){
                    prevPos = new LatLng(location.getLatitude(), location.getLongitude());
                    drawSignal();
                }else{
                    Location.distanceBetween(prevPos.latitude,prevPos.longitude,
                            location.getLatitude(),location.getLongitude(),distance);
                    if(distance[0] >= 15) {
                        prevPos = new LatLng(location.getLatitude(), location.getLongitude());
                        drawSignal();
                    }
                }

            }
        }
    }
    @Override
    public void onMapClick(LatLng latLng) {
        // TODO 맵 클릭(짧게) 이벤트 처리
    }
    @Override
    public void onMapLongClick(LatLng latLng) {
        // TODO 맵 클릭(길게) 이벤트 처리
    }
    @Override
    public boolean onMarkerClick(Marker marker) {
        // TODO 마커 클릭 이벤트 처리
        if(!mSignalInfoList.isEmpty()){
            for(SignalInfo signal : mSignalInfoList){
                if(signal.getMACAddress().equals(marker.getTitle())){
                    if(!signal.isVisible()){
                        String[] query2 = {"SELECT Latitude, Longitude, SSID, RSSI FROM LocalData where MAC = '"+ signal.getMACAddress() + "';",
                                "SELECT Latitude, Longitude, SSID, RSSI FROM WifiData where MAC = '"+ signal.getMACAddress() + "';" };
                        for(int i=0; i<2; i++){
                            Cursor cursor2 = mDatabaseRead.rawQuery(query2[i],null);
                            while(cursor2.moveToNext()){
                                signal.setSignal(new LatLng(cursor2.getFloat(cursor2.getColumnIndex("Latitude")), cursor2.getFloat(cursor2.getColumnIndex("Longitude"))));
                            }
                            cursor2.close();
                        }
                    }
                    signal.setVisible();
                }
            }
        }

        if(mIsAutoConnection) {
            // TODO 자동 연결시 필터링 기능, 필요하다면 따로 필터링 플래그를 두는것도 괜찮음

        } else {
            // TODO 수동 연결시 연결 기능
        }
        return true;
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = Google Map API = = = = = = = = = =

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = Draw Signal = = = = = = = = = =
    class SignalInfo{
        private String ssid;
        private String MACAddress;
        private LatLng APPoint;
        private Circle APRange;
        private Marker AP;
        private List<Circle> signalPoints;
        private int color;
        private boolean isVisible = false;

        public SignalInfo(){
            signalPoints = new ArrayList<>();
        }

        public void setSSID(String ssid){
            this.ssid = ssid;
        }

        public void setMACAddress(String mac){
            this.MACAddress = mac;
        }

        public void setAPPoint(LatLng center){
            this.APPoint = center;
            this.APRange = mMap.addCircle(new CircleOptions().center(APPoint)
                    .radius(10).strokeWidth(1).strokeColor(Color.argb(100,180,60,60))
                    .fillColor(Color.argb(100,180,60,60)).visible(isVisible));
            this.AP = mMap.addMarker(new MarkerOptions().position(APPoint)
                    .title(MACAddress).icon(BitmapDescriptorFactory.fromResource(R.drawable.wifi_logo)).visible(true));
        }

        public void setColor(int color){
            this.color = color;
        }

        public boolean isVisible(){ return isVisible;}

        public String getSSID(){
            return ssid;
        }

        public String getMACAddress(){
            return MACAddress;
        }

        public void setVisible(){
            if(isVisible){
                isVisible = false;
            }else{
                isVisible = true;
            }
            APRange.setVisible(isVisible);

            for(Circle circle : signalPoints){
                circle.setVisible(isVisible);
            }
        }

        public void setSignal(LatLng point){
            this.signalPoints.add(mMap.addCircle(new CircleOptions()
                    .radius(3).strokeWidth(1).strokeColor(color).fillColor(color)
                    .center(point).visible(isVisible)));
        }
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = Draw Signal = = = = = = = = = =

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = Draw Signal = = = = = = = = = =
    private void drawSignal(){
        mMap.clear();
        mSignalInfoList = new ArrayList<>();

        float curLat = (float)prevPos.latitude;
        float curLng = (float)prevPos.longitude;
        Random random = new Random();

        String[] query = { "SELECT MAC, SSID, Latitude, Longitude FROM LocalDevice where Latitude between '"+ String.format("%.6f",curLat-0.0005f) +"' and '" +  String.format("%.6f",curLat+0.0005f) + "'" +
                "and Longitude between '"+ String.format("%.6f",curLng-0.0005f) +"' and '" +  String.format("%.6f",curLng+0.0005f) + "';",
                "SELECT MAC, SSID, Latitude, Longitude FROM WifiDevice where Latitude between '"+ String.format("%.6f",curLat-0.0005f) +"' and '" +  String.format("%.6f",curLat+0.0005f) + "'" +
                        "and Longitude between '"+ String.format("%.6f",curLng-0.0005f) +"' and '" +  String.format("%.6f",curLng+0.0005f) + "';" };

//        PolygonOptions polygonOptions = new PolygonOptions().strokeWidth(5)
//                .add( new LatLng(((double)curLat-0.001),((double)curLng-0.001)), new LatLng(((double)curLat-0.001),((double)curLng+0.001)),
//                        new LatLng(((double)curLat+0.001),((double)curLng+0.001)), new LatLng(((double)curLat+0.001),((double)curLng-0.001)));
//        mMap.addPolygon(polygonOptions);

        int idx = 0;
        for(int i=0; i<2; i++){
            Cursor cursor = mDatabaseRead.rawQuery(query[i], null);
            // AP마다 색깔을 랜덤으로 하기 위해서

            while (cursor.moveToNext()) { //  ap
                String mac = cursor.getString(cursor.getColumnIndex("MAC"));
                String ssid = cursor.getString(cursor.getColumnIndex("SSID"));
                double lat = cursor.getFloat(cursor.getColumnIndex("Latitude"));
                double lng = cursor.getFloat(cursor.getColumnIndex("Longitude"));

                mSignalInfoList.add(new SignalInfo());
                mSignalInfoList.get(idx).setSSID(ssid);
                mSignalInfoList.get(idx).setMACAddress(mac);
                int color = Color.argb(30, random.nextInt(256), random.nextInt(256), random.nextInt(256));
                mSignalInfoList.get(idx).setColor(color);
                mSignalInfoList.get(idx).setAPPoint(new LatLng(lat, lng));
                idx++;
            }
            cursor.close();
        }
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = Draw Signal = = = = = = = = = =

    // = = = = = = = = = = 사이드 메뉴 (DrawerLayout) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if(mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // TODO ?
        // if you want to handle your other action bar items...
        return super.onOptionsItemSelected(item);
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 사이드 메뉴 (DrawerLayout) = = = = = = = = = =

    // = = = = = = = = = = 사이드 메뉴 리스트 (ListView) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    private ArrayList<String> mDrawerListItems;
    private String mSelectedItem;
    private ListView mDrawerList;
    private ArrayAdapter mDrawerListAdapter;

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mSelectedItem = mDrawerListItems.get(position);
            if(mSelectedItem == getResources().getString(R.string.wifi_setting)) {
                // [DONE] wifi setting과 관련된 리스트를 보여준다
                setDrawerListItems();
            } else if (mSelectedItem == getResources().getString(R.string.editor)) {
                // [DONE] 새로운 에디터 액티비티를 실행한다.
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                Intent intent = new Intent(visual_wifi_map.this, editor.class);
                intent.putExtra(getResources().getString(R.string.position), mMap.getCameraPosition());
                startActivity(intent);
            } else if(mSelectedItem == getResources().getString(R.string.wifi_synchronize)) {
                // [] wifi 동기화하는 서비스를 실행한다.
                // - - - - - - - - - - WIFI 동기화 서비스 시작(자동 종료) - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                Intent synchronize = new Intent(visual_wifi_map.this, wifi_setting_synchronize.class);
                startService(synchronize);
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                setDrawerListItems();
            } else if(mSelectedItem == getResources().getString(R.string.wifi_upload)) {
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                setDrawerListItems();
                // - - - - - - - - - - WIFI 업로드 선택 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                alertWifiUpload();
            } else if(mSelectedItem == getResources().getString(R.string.wifi_auto)) {
                mIsAutoConnection = true;
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                setDrawerListItems();
                // - - - - - - - - - - WIFI 자동연결 서비스 시작 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                mAutoConnectionServiceIntent = new Intent(visual_wifi_map.this, wifi_connection_auto.class);
                mAutoConnectionServiceName = startService(mAutoConnectionServiceIntent);
            } else if(mSelectedItem == getResources().getString(R.string.wifi_auto_off)) {
                mIsAutoConnection = false;
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                setDrawerListItems();
                // - - - - - - - - - - WIFI 자동연결 서비스 종료 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                try {
                    if(mAutoConnectionServiceIntent == null) {
                        Class serviceClass = Class.forName(mAutoConnectionServiceName.getClassName());
                        stopService(new Intent(visual_wifi_map.this, serviceClass));
                    } else {
                        stopService(mAutoConnectionServiceIntent);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else if(mSelectedItem == getResources().getString(R.string.wifi_manual)) {
                mWifiManager.startScan();
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                // - - - - - - - - - - WIFI 자동연결 서비스 종료 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                try {
                    if(mIsAutoConnection) {
                        if (mAutoConnectionServiceIntent == null) {
                            Class serviceClass = Class.forName(mAutoConnectionServiceName.getClassName());
                            stopService(new Intent(visual_wifi_map.this, serviceClass));
                        } else {
                            stopService(mAutoConnectionServiceIntent);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    mIsAutoConnection = false;
                }
                // - - - - - - - - - - WIFI 수동연결 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                setDrawerListItems();
                alertWifiManualList();
            } else if(mSelectedItem == getResources().getString(R.string.back)) {
                setDrawerListItems();
            } else if(mSelectedItem == getResources().getString(R.string.wifi_collector_run)) {
                mIsInformationCollection = true;
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                setDrawerListItems();
                // - - - - - - - - - - WIFI 정보 수집 서비스 시작 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                mInformationCollectorIntent = new Intent(visual_wifi_map.this, wifi_information_collector.class);
                mInformationCollectorName = startService(mInformationCollectorIntent);
            } else if(mSelectedItem == getResources().getString(R.string.wifi_collector_stop)) {
                mIsInformationCollection = false;
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                setDrawerListItems();
                // - - - - - - - - - - WIFI 정보 수집 서비스 종료 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                try {
                    if(mInformationCollectorIntent == null) {
                        Class serviceClass = Class.forName(mInformationCollectorName.getClassName());
                        stopService(new Intent(visual_wifi_map.this, serviceClass));
                    } else {
                        stopService(mInformationCollectorIntent);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else if(mSelectedItem == getResources().getString(R.string.guide)) {
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                Intent intent = new Intent(visual_wifi_map.this, Guide.class);
                startActivity(intent);
            }
        }
    }
    private void setDrawerListItems() {
        mDrawerListItems.clear();
        mDrawerListAdapter.notifyDataSetInvalidated();

        if(mSelectedItem == getResources().getString(R.string.wifi_setting)) {
            mDrawerListItems.add(getResources().getString(R.string.wifi_synchronize));
            mDrawerListItems.add(getResources().getString(R.string.wifi_upload));
            if(mIsAutoConnection) {
                mDrawerListItems.add(getResources().getString(R.string.wifi_auto_off));
            } else {
                mDrawerListItems.add(getResources().getString(R.string.wifi_auto));
            }
            mDrawerListItems.add(getResources().getString(R.string.wifi_manual));
//            if(mIsInformationCollection) {
//                mDrawerListItems.add(getResources().getString(R.string.wifi_collector_stop));
//            } else {
//                mDrawerListItems.add(getResources().getString(R.string.wifi_collector_run));
//            }
            mDrawerListItems.add(getResources().getString(R.string.back));
        } else {
            mDrawerListItems.add(getResources().getString(R.string.wifi_setting));
            mDrawerListItems.add(getResources().getString(R.string.editor));
            mDrawerListItems.add(getResources().getString(R.string.guide));
        }

        mDrawerListAdapter.notifyDataSetChanged();
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 사이드 메뉴 리스트 (ListView) = = = = = = = = = =

    // = = = = = = = = = = 액티비티 시작 (onCreate) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("[CREATE]", "------------------------------------");
        setContentView(R.layout.activity_visual_wifi_map);

        // - - - - - - - - - - 변수 복구 및 초기화 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // 위치 정보
        SharedPreferences pref = getSharedPreferences("SAVE_STATE", 0);
        mInitLocation_latitude = pref.getFloat("mInitLocation_latitude", 37.5f);
        mInitLocation_longitude = pref.getFloat("mInitLocation_longitude", 126.9f);
        mInitLocation_zoom = pref.getFloat("mInitLocation_zoom", 13f);
        // 서비스 정보
        mIsAutoConnection = false;
        mIsInformationCollection = false;
        mAutoConnectionServiceIntent = null;
        mInformationCollectorIntent = null;
        ActivityManager activityManager = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
        int breakpoint = 0;
        for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            Log.i("[SERVICE]", service.service.getClassName());
            if("com.capstone.theold4.visualwifi.wifi_connection_auto".equals(service.service.getClassName())) {
                Log.i("[INFO]", "find auto connection service");
                mAutoConnectionServiceName = service.service;
                mIsAutoConnection = true;
                breakpoint += 1;
            } else if("com.capstone.theold4.visualwifi.wifi_information_collector".equals(service.service.getClassName())) {
                Log.i("[INFO]", "find collection service");
                mInformationCollectorName = service.service;
                mIsInformationCollection = true;
                breakpoint += 1;
            }
            if(breakpoint >= 2) break;
        }
        // 데이터베이스 초기화
        mDatabaseManager = new database_manager(this);
        try {
            mDatabaseWrite = mDatabaseManager.getWritableDatabase();
            mDatabaseRead = mDatabaseManager.getReadableDatabase();
        } catch (SQLiteException e) {
            // TODO 데이터베이스 에러처리 필요
        }

        // - - - - - - - - - - Google Map (fragment) 초기화 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // - - - - - - - - - - 사이드 메뉴 (DrawerLayout) 초기화 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            // Called when a drawer has settled in a completely closed state.
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            // Called when a drawer has settled in a completely open state.
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        // Create toggle button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // - - - - - - - - - - 사이드 메뉴 리스트 (ListView) 초기화 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        mDrawerList = (ListView)findViewById(R.id.left_drawer);
        mDrawerListItems = new ArrayList<String>();
        // Set the adapter for the list view
        mDrawerListAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mDrawerListItems);
        mDrawerList.setAdapter(mDrawerListAdapter);
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        setDrawerListItems(); // this method must be run after settings

        // - - - - - - - - - - Information Collector 시작 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // TODO 이전상태 기억하여 서비스 종료시 자동 시작이 안되게끔 해야한다.
        if(!mIsInformationCollection) {
            mInformationCollectorIntent = new Intent(visual_wifi_map.this, wifi_information_collector.class);
            mInformationCollectorName = startService(mInformationCollectorIntent);
            mIsInformationCollection = true;
        }

        // - - - - - - - - - - 와이파이 수동연결 변수 초기화 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        mWifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        mWifiManager.startScan();
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 액티비티 시작 (onCreate) = = = = = = = = = =

    // = = = = = = = = = = 액티비티 재시작시 변수 복구 (onPause) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    @Override
    protected void onPause() {
        super.onPause();
        if(task != null){
            task.cancel(true);
        }
        Log.i("[PAUSE]", "------------------------------------");
        SharedPreferences pref = getSharedPreferences("SAVE_STATE", 0);
        SharedPreferences.Editor edit = pref.edit();
        try {
            LatLng position = mMap.getCameraPosition().target;
            mInitLocation_latitude = (float) position.latitude;
            mInitLocation_longitude = (float) position.longitude;
        } catch (Exception e) {
            e.printStackTrace();
        }
        edit.putFloat("mInitLocation_latitude", mInitLocation_latitude);
        edit.putFloat("mInitLocation_longitude", mInitLocation_longitude);
        edit.putFloat("mInitLocation_zoom", mInitLocation_zoom);
        edit.commit();
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 액티비티 재시작시 변수 복구 (onPause) = = = = = = = = = =

    public class UploadAsyncTask extends AsyncTask<String,String, Integer > {
        final String ServerIP = getResources().getString(R.string.server_ip);
        final int ServerPORT = getResources().getInteger(R.integer.server_port);
        final int TIMEOUT = getResources().getInteger(R.integer.connect_timeout);

        protected Integer doInBackground(String... param){

            try {
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(ServerIP, ServerPORT);
                socket.connect(socketAddress, TIMEOUT);
                if (!socket.isConnected()) {
                    Log.i("[SYNCHRONIZE]", "Connect socket fail");
                } else {
                    DataOutputStream dataOutputStream3 = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dataInputStream3 = new DataInputStream(socket.getInputStream());

                    // 사용할 변수들

                    String query3 = "SELECT * FROM WifiShare WHERE MAC='"+param[0]+"';";
                    Log.i("param", " asnctask's param : " + query3);
                    Cursor cursor = mDatabaseRead.rawQuery(query3, null);
                    int id_mac, id_ssid, id_pw, id_capability, id_date, id_time;
                    id_mac = cursor.getColumnIndex("MAC");
                    id_ssid = cursor.getColumnIndex("SSID");
                    id_pw = cursor.getColumnIndex("PW");
                    id_capability = cursor.getColumnIndex("CAPABILITY");
                    id_date = cursor.getColumnIndex("DATE");
                    id_time = cursor.getColumnIndex("TIME");
                    String data;
                    // - - - - - - - - - - send data from client to server - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                    Log.i("[SYNCHRONIZE]", "[Send share to server] [start]");
                    dataOutputStream3.writeUTF("Share_up");
                    cursor.moveToFirst();
                    data = "" + cursor.getString(id_mac) + "\t" + cursor.getString(id_ssid) + "\t" + cursor.getString(id_pw) +
                            "\t" + cursor.getString(id_capability) + "\t" + cursor.getInt(id_date) + "\t" + cursor.getInt(id_time);
                    dataOutputStream3.writeUTF(data);

                    Log.i("[SYNCHRONIZE]", "[Send share to server] [finish]");
                }
                socket.close();
            }
            catch(SQLiteException sqlE){

            }
            catch(IOException ioE){

            }

           return 0;
        }
    }

    public class APPointAsyncTask extends AsyncTask<LatLng, Double, List<LatLng>>{

        database_manager helper;
        SQLiteDatabase db_r, db_w;

        static final double quarterPI = Math.PI/4;
        static final double dir_EES = -Math.PI / 8;
        static final double dir_EEN = dir_EES + quarterPI;
        static final double dir_NNE = dir_EEN + quarterPI;
        static final double dir_NNW = dir_NNE + quarterPI;
        static final double dir_WWN = dir_NNW + quarterPI;
        static final double dir_WWS = -dir_WWN;
        static final double dir_SSW = dir_WWS + quarterPI;
        static final double dir_SSE = dir_SSW + quarterPI;

        String TAG = "CAPL";
        double progress = 0;
        double total = 0;

        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<LatLng> doInBackground(LatLng... params) {
            helper = new database_manager(visual_wifi_map.this);
            float EEx=0, WWx=0, SSx=0, NNx=0 , ENx=0, WNx=0, ESx=0, WSx=0; // 8방위 x좌표
            float EEy=0, WWy=0, SSy=0, NNy=0 , ENy=0, WNy=0, ESy=0, WSy=0; // 8방위 y좌표
            float EEd=0, WWd=0, SSd=0, NNd=0 , ENd=0, WNd=0, ESd=0, WSd=0; // 8방위까지의 max 거리
            float CCx=0, CCy=0, tempX=0, tempY=0, tempD=0;
            int tempRSSI;

            double angle;

            try {
                db_w = helper.getWritableDatabase();
                db_r = helper.getReadableDatabase();
                //데이터베이스 객체를 얻기 위하여 getWritableDatabse()를 호출
            } catch (SQLiteException e) {
            }

            try
            {
                publishProgress(new Double(progress));
                String sqlCount = "SELECT count(DISTINCT MAC) FROM LocalDevice;";
                Cursor count = db_r.rawQuery(sqlCount, null);
                while(count.moveToNext()){
                    total = count.getInt(0);
                }
                count.close();

                String sql1 = "SELECT MAC, Latitude, Longitude FROM LocalDevice;";
                Cursor c = db_r.rawQuery(sql1, null);
                while (c.moveToNext()) {
                    String _mac = c.getString(c.getColumnIndex("MAC"));
                    CCx = c.getFloat(c.getColumnIndex("Longitude"));
                    CCy = c.getFloat(c.getColumnIndex("Latitude"));

                    String[] sql2 = {"SELECT Latitude, Longitude FROM LocalData WHERE MAC = '" + _mac + "';",
                            "SELECT Latitude, Longitude FROM WifiData WHERE MAC = '" + _mac + "';"};
                    for (int sq = 0; sq < 2; sq++) {
                        Cursor c2 = db_r.rawQuery(sql2[sq], null);
                        while (c2.moveToNext()) {
                            tempY = c2.getFloat(c2.getColumnIndex("Latitude"));
                            tempX = c2.getFloat(c2.getColumnIndex("Longitude"));

                            // 중심으로의 상대적 위치 및 거리
                            tempY -= CCy;
                            tempX -= CCx;
                            tempD = tempX * tempX + tempY * tempY;

                            // 8방향  8점 잡기
                            if (tempX == 0 && tempY == 0) { // 센터이면
                                continue;
                            } else {
                                angle = Math.atan2(tempY, tempX);
                                if (angle >= dir_WWN) { // 서 (절반)
                                    if (tempD > WWd) {
                                        WWd = tempD;
                                        WWx = tempX;
                                        WWy = tempY;
                                    }
                                } else if (angle >= dir_NNW) { // 북서
                                    if (tempD > WNd) {
                                        WNd = tempD;
                                        WNx = tempX;
                                        WNy = tempY;
                                    }
                                } else if (angle >= dir_NNE) {  // 북
                                    if (tempD > NNd) {
                                        NNd = tempD;
                                        NNx = tempX;
                                        NNy = tempY;
                                    }
                                } else if (angle >= dir_EEN) {  // 북동
                                    if (tempD > ENd) {
                                        ENd = tempD;
                                        ENx = tempX;
                                        ENy = tempY;
                                    }
                                } else if (angle >= dir_EES) {  // 동
                                    if (tempD > EEd) {
                                        EEd = tempD;
                                        EEx = tempX;
                                        EEy = tempY;
                                    }
                                } else if (angle >= dir_SSE) {  // 남동
                                    if (tempD > ESd) {
                                        ESd = tempD;
                                        ESx = tempX;
                                        ESy = tempY;
                                    }
                                } else if (angle >= dir_SSW) {  // 남
                                    if (tempD > SSd) {
                                        SSd = tempD;
                                        SSx = tempX;
                                        SSy = tempY;
                                    }
                                } else if (angle >= dir_WWS) {  // 남서
                                    if (tempD > WSd) {
                                        WSd = tempD;
                                        WSx = tempX;
                                        WSy = tempY;
                                    }
                                } else {   //  서 (나머지 절반)
                                    if (tempD > WWd) {
                                        WWd = tempD;
                                        WWx = tempX;
                                        WWy = tempY;
                                    }
                                }
                            }  // if else 문, 8점 비교 대입
                        } // while문 Local, Wifi 테이블 8점 잡기
                        c2.close();
                    }  // for문 end 8점 잡기 완료 ( DB 모두 검색 완료 )

                    // 중점 잡기 ( AP 위치 잡기 )
                    CCx = CCx + (NNx + SSx + EEx + WWx + WNx + WSx + ENx + ESx) / 8;
                    CCy = CCy + (NNy + SSy + EEy + WWy + WNy + WSy + ENy + ESy) / 8;

                    // localDevice 테이블 업데이트하기
                    String sql3 = "UPDATE LocalDevice " +
                            "SET Latitude=" + CCy + ", Longitude="+ CCx +
                            " WHERE MAC = '" + _mac + "';";

                    db_w.execSQL(sql3);
                    db_w.execSQL("delete from WifiDevice where MAC = '" + _mac + "';");
                    progress++;
                    publishProgress(new Double(progress));

                }// while문 end  LocalDevice 1개 해당하는 루프
            }
            catch(SQLiteException sqlE){
                sqlE.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(Double... progress) {
            double ing = progress[0]/total;
            if(ing == 0){
                Toast.makeText(visual_wifi_map.this,"AP 위치 계산을 시작합니다.", Toast.LENGTH_SHORT).show();
            }else if(ing == 1){
                Toast.makeText(visual_wifi_map.this, "AP 위치 계산을 끝냈습니다.(총 " + total + "개)", Toast.LENGTH_SHORT).show();
                this.cancel(true);
            }
        }

        protected void onPostExecute(List<LatLng> APPoint) {
            super.onPostExecute(APPoint);
        }

        //--- AsyncTask.cancel(true) 호출시 실행되어 thread를 취소 합니다.
        protected void onCancelled() {
            checkComputeComplete = true;
            super.onCancelled();
        }
    }
}
