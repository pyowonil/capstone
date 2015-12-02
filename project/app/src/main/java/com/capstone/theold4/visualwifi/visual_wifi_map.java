package com.capstone.theold4.visualwifi;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Camera;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

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
    }
    @Override
    public void onMapLongClick(LatLng latLng) {
        // TODO 맵 클릭(길게) 이벤트 처리
    }
    @Override
    public boolean onMarkerClick(Marker marker) {
        // TODO 마커 클릭 이벤트 처리
        if(mIsAutoConnection) {
            // TODO 자동 연결시 필터링 기능
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
        // TODO
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
                setDrawerListItems();

            } else if(mSelectedItem == getResources().getString(R.string.filtering)) {

                mDrawerLayout.closeDrawer(mDrawerList); // closed
            } else if (mSelectedItem == getResources().getString(R.string.editor)) {
                mDrawerLayout.closeDrawer(mDrawerList); // closed
                Intent intent = new Intent(visual_wifi_map.this, editor.class);

                intent.putExtra(getResources().getString(R.string.position), mMap.getCameraPosition());
                startActivity(intent);
            } else if(mSelectedItem == getResources().getString(R.string.wifi_synchronize)) {

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
        SharedPreferences pref = getSharedPreferences("SAVE_STATE", 0);
        mInitLocation_latitude = pref.getFloat("mInitLocation_latitude", 37.5f);
        mInitLocation_longitude = pref.getFloat("mInitLocation_longitude", 126.9f);
        mInitLocation_zoom = pref.getFloat("mInitLocation_zoom", 13f);
        mIsAutoConnection = false;
        mAutoConnectionServiceIntent = null;
        ActivityManager activityManager = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            Log.i("[SERVICE]", service.service.getClassName());
            if("com.capstone.theold4.visualwifi.wifi_connection_auto".equals(service.service.getClassName())) {
                Log.i("[INFO]", "find service");
                final String WIFI_CONNECTION_AUTO = "com.capstone.theold4.visualwifi.wifi_connection_auto";
                mAutoConnectionServiceName = service.service;
                //mAutoConnectionServiceIntent = ((Intent) getSystemService(WIFI_CONNECTION_AUTO));
                mIsAutoConnection = true;
                break;
            }
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
