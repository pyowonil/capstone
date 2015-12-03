package com.capstone.theold4.visualwifi;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Camera;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.WifiManager;
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
import android.widget.ListView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

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
    private boolean mIsAutoConnection;
    private Intent mAutoConnectionServiceIntent;
    private ComponentName mAutoConnectionServiceName;
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = WIFI Connection = = = = = = = = = =

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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mInitLocation_latitude, mInitLocation_longitude), mInitLocation_zoom));
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
    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }
    @Override
    public void onMyLocationChange(Location location) {
        // TODO 위치 변경 이벤트 처리
    }
    @Override
    public void onMapClick(LatLng latLng) {
        // TODO 맵 클릭(짧게) 이벤트 처리

        // TODO WIFI 마커 찍기 --- 다른 적당한 위치에 구현해야 함
        // 간단하게 구현해봄 (데이터베이스와의 연동을 확인하기 위해서)
        {
            mMap.clear();
            String query = "SELECT * FROM LocalData;";
            Cursor cursor = mDatabaseRead.rawQuery(query, null);
            int id_mac, id_latitude, id_longitude, id_ssid, id_rssi, id_date, id_time;
            id_mac = cursor.getColumnIndex("MAC");
            id_latitude = cursor.getColumnIndex("Latitude");
            id_longitude = cursor.getColumnIndex("Longitude");
            id_ssid = cursor.getColumnIndex("SSID");
            id_rssi = cursor.getColumnIndex("RSSI");
            id_date = cursor.getColumnIndex("DATE");
            id_time = cursor.getColumnIndex("TIME");
            String data;

            while (cursor.moveToNext()) {
                data = "" + cursor.getString(id_mac) + " " + cursor.getFloat(id_latitude) + " " + cursor.getFloat(id_longitude) +
                        " " + cursor.getString(id_ssid) + " " + cursor.getInt(id_rssi) + " " + cursor.getInt(id_date) +
                        " " + cursor.getInt(id_time);
                Log.i("[DATALOADING]", data);
                LatLng latLngData = new LatLng(cursor.getDouble(id_latitude), cursor.getDouble(id_longitude));
                CircleOptions circleOptions = new CircleOptions().radius(1).strokeWidth(1).strokeColor(Color.argb(150, 0, 50, 200)).fillColor(Color.argb(150, 0, 50, 170)).center(latLngData);
                MarkerOptions markerOptions = new MarkerOptions().visible(true).draggable(false).position(latLngData).title(cursor.getString(id_ssid));
                mMap.addCircle(circleOptions);
                //mMap.addMarker(markerOptions);
            }
        }
    }
    @Override
    public void onMapLongClick(LatLng latLng) {
        // TODO 맵 클릭(길게) 이벤트 처리
    }
    @Override
    public boolean onMarkerClick(Marker marker) {
        // TODO 마커 클릭 이벤트 처리
        if(mIsAutoConnection) {
            // TODO 자동 연결시 필터링 기능, 필요하다면 따로 필터링 플래그를 두는것도 괜찮음
        } else {
            // TODO 수동 연결시 연결 기능
        }
        return true;
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = Google Map API = = = = = = = = = =

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
            } else if(mSelectedItem == getResources().getString(R.string.filtering)) {
                // [] filtering과 관련된 리스트를 보여준다.
                // TODO 아래의 closeDrawer부분에 새로운 리스트를 보여주는 코드 필요
                mDrawerLayout.closeDrawer(mDrawerList); // closed
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
            } else if(mSelectedItem == getResources().getString(R.string.wifi_auto)) {
                mIsAutoConnection = true;
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                setDrawerListItems();
                // - - - - - - - - - - WIFI 자동연결 서비스 시작 - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                mAutoConnectionServiceIntent = new Intent(visual_wifi_map.this, wifi_connection_auto.class);
                mAutoConnectionServiceName = startService(mAutoConnectionServiceIntent);
            } else if(mSelectedItem == getResources().getString(R.string.wifi_manual)) {
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
                mDrawerListItems.add(getResources().getString(R.string.wifi_manual));
            } else {
                mDrawerListItems.add(getResources().getString(R.string.wifi_auto));
            }
            if(mIsInformationCollection) {
                mDrawerListItems.add(getResources().getString(R.string.wifi_collector_stop));
            } else {
                mDrawerListItems.add(getResources().getString(R.string.wifi_collector_run));
            }
            mDrawerListItems.add(getResources().getString(R.string.back));
        } else {
            mDrawerListItems.add(getResources().getString(R.string.wifi_setting));
            mDrawerListItems.add(getResources().getString(R.string.filtering));
            mDrawerListItems.add(getResources().getString(R.string.editor));
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
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 액티비티 시작 (onCreate) = = = = = = = = = =

    // = = = = = = = = = = 액티비티 재시작시 변수 복구 (onPause) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("[PAUSE]", "------------------------------------");
        SharedPreferences pref = getSharedPreferences("SAVE_STATE", 0);
        SharedPreferences.Editor edit = pref.edit();
        LatLng position = mMap.getCameraPosition().target;
        mInitLocation_latitude = (float) position.latitude;
        mInitLocation_longitude = (float) position.longitude;
        edit.putFloat("mInitLocation_latitude", mInitLocation_latitude);
        edit.putFloat("mInitLocation_longitude", mInitLocation_longitude);
        edit.putFloat("mInitLocation_zoom", mInitLocation_zoom);
        edit.commit();
    }
//    @Override
//    protected void onStop() {
//        super.onStop();
//        Log.i("[STOP]", "------------------------------------");
//        SharedPreferences pref = getSharedPreferences("SAVE_STATE", 0);
//        SharedPreferences.Editor edit = pref.edit();
//        LatLng position = mMap.getCameraPosition().target;
//        mInitLocation_latitude = (float) position.latitude;
//        mInitLocation_longitude = (float) position.longitude;
//        edit.putFloat("mInitLocation_latitude", mInitLocation_latitude);
//        edit.putFloat("mInitLocation_longitude", mInitLocation_longitude);
//        edit.putFloat("mInitLocation_zoom", mInitLocation_zoom);
//        edit.commit();
//    }
//    @Override
//    protected void onResume() {
//        super.onResume();
//        Log.i("[RESUME]","------------------------------------");
//        SharedPreferences pref = getSharedPreferences("SAVE_STATE", 0);
//        mInitLocation_latitude = pref.getFloat("mInitLocation_latitude", 37.5f);
//        mInitLocation_longitude = pref.getFloat("mInitLocation_longitude", 126.9f);
//        mInitLocation_zoom = pref.getFloat("mInitLocation_zoom", 13f);
//        mIsAutoConnection = false;
//        ActivityManager activityManager = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
//        for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
//            Log.i("[SERVICE]", service.service.getClassName());
//            if("com.capstone.theold4.visualwifi.wifi_connection_auto".equals(service.service.getClassName())) {
//                Log.i("[INFO]", "find service");
//                final String WIFI_CONNECTION_AUTO = "com.capstone.theold4.visualwifi.wifi_connection_auto";
//                mAutoConnectionServiceIntent = (Intent) getSystemService(WIFI_CONNECTION_AUTO);
//                mIsAutoConnection = true;
//                break;
//            }
//        }
//        if(!mIsAutoConnection) {
//            mAutoConnectionServiceIntent = null;
//        }
//    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 액티비티 재시작시 변수 복구 (onPause) = = = = = = = = = =
}
