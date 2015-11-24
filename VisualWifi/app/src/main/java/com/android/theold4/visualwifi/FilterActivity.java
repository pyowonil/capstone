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
 * 목적 : 필터링 액티비티 *
 *
 *
 */

public class FilterActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {
    // 구글맵 관련 선언
    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    static final LatLng SEOUL = new LatLng(37.56, 126.97);

    // 사이드 메뉴 관련 선언
    private String[] navItems = {"기능1", "기능2", "돌아가기"}; // 필터링 기능 + 메인으로 돌아가기
    private ListView lvNavList;
    private FrameLayout flContainer;

    // 사이드 메뉴 토글 관련
    private DrawerLayout dlDrawer;
    private ActionBarDrawerToggle dtToggle;



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
                case 0:   // 기능 1

                    break;
                case 1:  // 기능 2

                    break;
                case 2:  // 돌아가기
                    finish();
                    break;
            }
            dlDrawer.closeDrawer(lvNavList); // 선택 후 사이드 메뉴 닫기
        }
    }
    // 사이드 메뉴 토글 관련
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

    @Override
    public void onClick(View v) {

    }
}