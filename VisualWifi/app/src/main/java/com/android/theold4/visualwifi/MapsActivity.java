package com.android.theold4.visualwifi;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.location.LocationListener;

import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/*
 * 목적 : 메인 액티비티 *
 * 내용 : 구글맵과 사이드메뉴
 *
 * 구성 :
 *      액티비티           - MapsActivity.java , WifiSettingActivity.java
 *      서비스             - VWService.java , AutoConnectService.java
 *      브로드캐스트리시버 - NetWatcher.java
 *      클래스             - DBManager.java(미정)
 *
 * 기능 :
 *      DB관련 함수 콜    - VWService.java , WifiSettingActivity.java
 *      서버관련 통신     - WifiSettingActivity.java
 *      Wifi상태 검사     - NetWatcher.java
 *      자동연결          - AutoConnectService.java
 *
 */

//public class MapsActivity extends AppCompatActivity implements LocationListener{
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {
    // 구글맵 관련 선언
    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    static final LatLng SEOUL = new LatLng(37.56, 126.97);

    // 사이드 메뉴 관련 선언
    private String[] navItems = {"Wifi설정", "필터링", "에디터"}; // wifi설정에 동기화,업로드, 자동수동 설정
    private ListView lvNavList;
    private FrameLayout flContainer;

    // 사이드 메뉴 토글 관련
    private DrawerLayout dlDrawer;
    private ActionBarDrawerToggle dtToggle;

    // Wifi 설정 메뉴 액티비티 화면 관련
    public static final int REQUEST_CODE_WiFiSetting = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        InitGoogleMap(); // 구글맵 초기화
        InitSideMenu();  // 사이드메뉴 및 토글버튼 초기화
    }

    // *********************************************************************
    // ----------------------- 구글 맵 관련 start --------------------------
    private void InitGoogleMap() {
        // 구글 맵 관련
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // onMapReady() 함수 호출 하게됨 (시드니 마커 추가)
        map = mapFragment.getMap();
        //현재 위치로 가는 버튼 표시
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SEOUL, 13));


    }

    private void drawMarker(Location location) {
        // 기존 마커 지우기
        map.clear();
        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

        // currentPosition 위치로 카메라 중심을 옮기고
        // 화면줌 (2~21) 조정 클수록 확대
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 17));
        map.animateCamera(CameraUpdateFactory.zoomTo(17), 2000, null);
        // 마커 추가
        map.addMarker(new MarkerOptions()
                .position(currentPosition)
                .snippet("Lat:" + location.getLatitude() + " Lng:" + location.getLongitude())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title("현재위치"));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        map.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney));

    }

    // ----------------------- 구글 맵 관련 End --------------------------------

    // *************************************************************************
    // ----------------------- 사이드 메뉴 관련 start --------------------------
    private void InitSideMenu(){
        // 사이드 메뉴 관련
        lvNavList = (ListView)findViewById(R.id.lv_activity_main_nav_list);
        flContainer = (FrameLayout)findViewById(R.id.fl_activity_main_container);

        lvNavList.setAdapter(
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, navItems));
        lvNavList.setOnItemClickListener(new DrawerItemClickListener());

        // 사이드 메뉴 토글버튼 관련
        dlDrawer = (DrawerLayout)findViewById(R.id.dl_activity_main_drawer);
        dtToggle = new ActionBarDrawerToggle(this,dlDrawer,R.string.open,R.string.close){
            public void onDrawerClosed(View drawerView){
                super.onDrawerClosed(drawerView);
            }
            public void onDrawerOpened(View drawerView){
                super.onDrawerOpened(drawerView);
            }
        };
        dlDrawer.setDrawerListener(dtToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }



    private class DrawerItemClickListener implements ListView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> adapter, View view, int position, long id){
            switch(position){
                case 0:
                    Log.i("abc","0번");
                    Intent intent = new Intent(getApplicationContext(), WifiSettingActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_WiFiSetting );

                    break;
                case 1:
                    Log.i("abc","1번");
                    break;
                case 2:
                    Log.i("abc","2번");
                    break;
            }
            dlDrawer.closeDrawer(lvNavList); // 선택 후 사이드 메뉴 닫기
        }
    }
    // 토글 관련
    protected  void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        dtToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        dtToggle.onConfigurationChanged(newConfig);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(dtToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // ----------------------- 사이드 메뉴 관련 End --------------------------

    // ----------------------- 액티비티 관련 Start ---------------------------
    @Override
    public void onClick(View v) {

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);

        if( requestCode == REQUEST_CODE_WiFiSetting){
            Toast toast = Toast.makeText(getBaseContext(), "onActivityResult 메소드가 호출됨. 코드 : "+requestCode+
                    ", 결과 코드 : " + resultCode, Toast.LENGTH_LONG);
            toast.show();

            if(resultCode == RESULT_OK){
                String name = intent.getExtras().getString("setting");
                toast = Toast.makeText(getBaseContext(), "응답 : " + name, Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }
    // ----------------------- 액티비티 관련 End -----------------------------
}