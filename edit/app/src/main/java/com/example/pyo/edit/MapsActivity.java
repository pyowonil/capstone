/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.pyo.edit;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

public class MapsActivity extends AppCompatActivity
        implements
        OnMapReadyCallback, // 구글 맵 준비 (기본)
        GoogleMap.OnMyLocationButtonClickListener,  // 자신의 위치로 가는 버튼
        GoogleMap.OnMapClickListener, // 맵 클릭 리스너
        GoogleMap.OnMapLongClickListener, // 맵 클릭 리스너
        GoogleMap.OnMarkerDragListener, // 마커 드래그 리스너
        ActivityCompat.OnRequestPermissionsResultCallback // Permission Request Callback 기능
{

    // Request code for location permission request.
    // #onRequestPermissionResult(int, String[], int[])
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    // Flag indicating whether a requested permission has been denied after returning in
    // {#onRequestPermissionsResult(int, String[], int[])}
    private boolean mPermissionDenied = false;

    private GoogleMap mMap;
    private UiSettings mUiSettings;
    // 그리기 작업을 하기 위해서는 지도의 움직임을 제한해야 한다.
    private boolean mLock=true;

    private List<DraggableCircle> mCircles = new ArrayList<DraggableCircle>(1);
    private FrameLayout mDrawMap;
    private boolean IS_MAP_MOVEABLE;
    private Vector<Vector<Point>> mDrawPoints;
    private DrawCanvas mDrawCanvas;

    // 디버깅용 택스트
    private TextView mInfoTap;

    private double mDeviceRange = 100;
    private enum mMode {DEFAULT, DRAW, LOAD, RUN, EXIT};
    private mMode mCurrentMode = mMode.DEFAULT;


    // 원을 관리하는 클래스
    private class DraggableCircle {
        private static final int WIDTH = 0; // ~50
        private static final int HUE = 0; // ~360
        private static final int ALPHA = 30; // ~255
        private final Marker centerMarker;
        private final Marker radiusMarker;
        private final Circle circle;
        private double radius;

        public DraggableCircle(LatLng center, double radius) {
            this.radius = radius;
            centerMarker = mMap.addMarker(new MarkerOptions().position(center).draggable(true));
            radiusMarker = mMap.addMarker(new MarkerOptions().position(toRadiusLatLng(center, radius)).draggable(true).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            centerMarker.setTitle(Double.toString(radius));
            circle = mMap.addCircle(new CircleOptions().center(center).radius(radius).strokeWidth(WIDTH).strokeColor(Color.BLACK).fillColor(Color.HSVToColor(ALPHA, new float[]{HUE, 1, 1})));
        }

        public boolean onMarkerMoved(Marker marker) {
            if (marker.equals(centerMarker)) {
                circle.setCenter(marker.getPosition());
                radiusMarker.setPosition(toRadiusLatLng(marker.getPosition(), radius));
                centerMarker.setTitle(Double.toString(radius));
                return true;
            }
            if (marker.equals(radiusMarker)) {
                radius = toRadiusMeters(centerMarker.getPosition(), radiusMarker.getPosition());
                circle.setRadius(radius);
                centerMarker.setTitle(Double.toString(radius));
                return true;
            }
            return false;
        }

        public void markerDraggable(boolean b) {
            centerMarker.setDraggable(b);
            radiusMarker.setDraggable(b);
            centerMarker.setVisible(b);
            radiusMarker.setVisible(b);
        }

        public void remove() {
            centerMarker.remove();
            radiusMarker.remove();
            circle.remove();
        }

        public LatLng getCenter() {
            return centerMarker.getPosition();
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        onMarkerMoved(marker);
    }
    @Override
    public void onMarkerDragEnd(Marker marker) {
        onMarkerMoved(marker);
    }
    public void onMarkerDrag(Marker marker) {
        onMarkerMoved(marker);
    }
    public void onMarkerMoved(Marker marker) {
        for (DraggableCircle draggableCircle : mCircles) {
            if (draggableCircle.onMarkerMoved(marker)) {
                break;
            }
        }
    }

    private static final double RADIUS_OF_EARTH_METERS = 6371009;
    // Generate LatLng of radius marker
    private static LatLng toRadiusLatLng(LatLng center, double radius) {
        double radiusAngle = Math.toDegrees(radius / RADIUS_OF_EARTH_METERS) / Math.cos(Math.toRadians(center.latitude));
        return new LatLng(center.latitude, center.longitude + radiusAngle);
    }
    private static double toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude, radius.latitude, radius.longitude, result);
        return result[0];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mInfoTap = (TextView)findViewById(R.id.tap);

        // REGISTER DRAW MAP TOUCH LISENTER
        mDrawMap = (FrameLayout)findViewById(R.id.draw_map);
        IS_MAP_MOVEABLE = false;
        mDrawPoints = new Vector<Vector<Point>>();
        mDrawMap.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Point point = new Point(Math.round(event.getX()), Math.round(event.getY()));
                if(IS_MAP_MOVEABLE) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mDrawPoints.add(new Vector<Point>());
                            mDrawPoints.lastElement().add(point);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            mDrawPoints.lastElement().add(point);
                            break;
                        case MotionEvent.ACTION_UP:
                            //mDrawPoints.clear();
                            break;
                    }
                }

                System.out.println("[!] drag event : " + point.toString());

                return IS_MAP_MOVEABLE;
            }
        });
        mDrawCanvas = new DrawCanvas(this);
        mDrawMap.addView(mDrawCanvas);
    }

    class DrawCanvas extends View {
        private Paint mPaint;
        private Bitmap mBitmap;

        public DrawCanvas(Context context) {
            super(context);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(10);
            mPaint.setColor(Color.BLACK);

            this.setDrawingCacheEnabled(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            synchronized (this) {
                super.onDraw(canvas);

                if(mCurrentMode == mMode.RUN) {
                    canvas.drawBitmap(mBitmap,0,0,null);
                }

                mPaint.setColor(Color.RED);
                // Draw Circle
                for (DraggableCircle circle : mCircles) {
                    Point center = mMap.getProjection().toScreenLocation(circle.getCenter());
                    canvas.drawCircle(center.x, center.y, 5, mPaint);
                }

                mPaint.setColor(Color.BLACK);
                // Draw Line
                if (mDrawPoints.isEmpty()) {
                    ;
                } else {
                    for (Vector<Point> points : mDrawPoints) {
                        Point a = points.get(0);
                        int size = points.size();
                        for (int i = 1; i < size; i++) {
                            Point b = points.get(i);
                            canvas.drawLine(a.x, a.y, b.x, b.y, mPaint);
                            a = b;
                        }
                    }
                }
                invalidate();

            }
        }

        public void simulate() {
            this.buildDrawingCache();
            mBitmap = Bitmap.createBitmap(this.getDrawingCache());
            this.setDrawingCacheEnabled(false);

            // TODO 시뮬레이트

            this.setDrawingCacheEnabled(true);
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mUiSettings = mMap.getUiSettings();
        mUiSettings.setAllGesturesEnabled(mLock);

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);

        enableMyLocation();
    }

    // Enables the My Location layer if the fine location permission has been granted.
    private void enableMyLocation() {
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
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

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

    @Override
    public void onMapClick(LatLng latLng) {
        Point point = mMap.getProjection().toScreenLocation(latLng);
        mInfoTap.setText("Tapped : " + latLng + "\nPoint : " + point.toString());
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mInfoTap.setText("Long Tapped : " + latLng);

        // if load mode then create circle
        if (mCurrentMode == mMode.LOAD) {
            DraggableCircle circle = new DraggableCircle(latLng, mDeviceRange);
            mCircles.add(circle);
        }
    }

    public void onClickDraw(View view) {
        mLock = false;
        mUiSettings.setAllGesturesEnabled(mLock);
        IS_MAP_MOVEABLE = true;
        mCurrentMode = mMode.DRAW;
        for(DraggableCircle circle : mCircles) {
            circle.markerDraggable(false);
        }
    }
    public void onClickLoad(View view) {
        mLock = true;
        mUiSettings.setAllGesturesEnabled(mLock);
        IS_MAP_MOVEABLE = false;
        mCurrentMode = mMode.LOAD;
        for(DraggableCircle circle : mCircles) {
            circle.markerDraggable(true);
        }
    }
    public void onClickClear(View view) {
        IS_MAP_MOVEABLE = false;
        mDrawPoints.clear();
        for(DraggableCircle circle : mCircles) {
            circle.remove();
        }
        mCircles.clear();
        mLock = true;
        mUiSettings.setAllGesturesEnabled(mLock);
        mCurrentMode = mMode.DEFAULT;
    }
    public void onClickRun(View view) {
        IS_MAP_MOVEABLE = false;
        for(DraggableCircle circle : mCircles) {
            circle.markerDraggable(false);
        }
        // TODO
        // 실제 선정된 기기를 통한 연산 필요
        mDrawCanvas.simulate();
        mCurrentMode = mMode.RUN;
    }
    public void onClickExit(View view) {
        IS_MAP_MOVEABLE = false;
        mCurrentMode = mMode.EXIT;
        // TODO
        // Activity 전환 필요
    }

}
