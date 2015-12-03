package com.capstone.theold4.visualwifi;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

public class editor extends AppCompatActivity
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

    private Intent intent;
    private GoogleMap mMap;
    private UiSettings mUiSettings;
    // 그리기 작업을 하기 위해서는 지도의 움직임을 제한해야 한다.
    private boolean mLock=true;

    private List<DraggableCircle> mCircles = new ArrayList<DraggableCircle>(1);
    private FrameLayout mDrawMap;
    private boolean IS_MAP_MOVEABLE;
    private Vector<Vector<Point>> mDrawPoints;
    private DrawCanvas mDrawCanvas;

    private double mDeviceRange = 100;
    private enum mMode {DEFAULT, DRAW, LOAD, RUN, EXIT};
    private mMode mCurrentMode = mMode.DEFAULT;

    private ListView mDeviceListView;
    private ArrayList<CVData> mDeviceList;
    private DataAdapter mAdapter = null;
    private int mDeviceNum = -1;

    // 원을 관리하는 클래스
    private class DraggableCircle {
        private static final int WIDTH = 0; // ~50
        private static final int HUE = 0; // ~360
        private static final int ALPHA = 30; // ~255
        private final Marker centerMarker;
        private final Marker radiusMarker;
        private final Circle circle;
        private double radius;
        private Bitmap icon;
        private int iconSize = 100;

        public DraggableCircle(LatLng center, double radius) {
            if(mDeviceNum == 0){
                icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.device01);
                icon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, false);
            }else if(mDeviceNum == 1){
                icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.device02);
                icon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, false);
            }else if(mDeviceNum == 2){
                icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.device03);
                icon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, false);
            }else if(mDeviceNum == 3){
                icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.device04);
                icon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, false);
            }else if(mDeviceNum == 4){
                icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.device05);
                icon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, false);
            }

            this.radius = radius;
            centerMarker = mMap.addMarker(new MarkerOptions().position(center).draggable(true).icon(BitmapDescriptorFactory.fromBitmap(icon)));
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
        public double getRadius() { return radius; }
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
        setContentView(R.layout.activity_editor);

        intent = getIntent();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDeviceListView = (ListView)findViewById(R.id.deviceListView);
        mDeviceList = new ArrayList<CVData>();//객체 생성
        mAdapter = new DataAdapter(this, mDeviceList);//데이터를 받기위한 데이터어댑터 객체
        mDeviceListView.setAdapter(mAdapter); //리스트뷰에 어댑터 연결
        mDeviceListView.setBackgroundColor(Color.WHITE);

        // CVData클래스를 만들 때 순서대로 해당 인수값을 입력
        mAdapter.add(new CVData(getApplicationContext(), "Device01",
                "EFM ipTIME A3004-dual 유무선공유기", R.drawable.device01));
        mAdapter.add(new CVData(getApplicationContext(), "Device02",
                "TP-LINK Archer C7 AC1750 유무선공유기", R.drawable.device02));
        mAdapter.add(new CVData(getApplicationContext(), "Device03",
                "ASUS RT-AC68U 유무선공유기", R.drawable.device03));
        mAdapter.add(new CVData(getApplicationContext(), "Device04",
                "D-Link DIR-850L 유무선공유기", R.drawable.device04));
        mAdapter.add(new CVData(getApplicationContext(), "Device05",
                "TP-LINK Archer C2 AC750 유무선공유기", R.drawable.device05));

        mDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDeviceNum = position;
                mDeviceRange = 10;
                mDeviceListView.setVisibility(View.INVISIBLE);
            }
        });
        mDeviceListView.setVisibility(View.INVISIBLE);

        // REGISTER DRAW MAP TOUCH LISENTER
        mDrawMap = (FrameLayout)findViewById(R.id.draw_map);
        IS_MAP_MOVEABLE = false;
        mDrawPoints = new Vector<Vector<Point>>();
        mDrawMap.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Point point = new Point(Math.round(event.getX()), Math.round(event.getY()));
                if (IS_MAP_MOVEABLE) {
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

            // TODO 시뮬레이트 쓰레드화 필요
            // 비트맵 전체 크기 계산 및 위도 <-> 디바이스 좌표 변환용 프로젝션 생성
            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();
            Projection projection = mMap.getProjection();

            // 하나의 디바이스마다 계산 (이터레이터 이용)
            for(DraggableCircle circle : mCircles) {
                // 관심 구역 설정
                Point center = projection.toScreenLocation(circle.getCenter());
                Point radius = projection.toScreenLocation(toRadiusLatLng(circle.getCenter(), circle.getRadius()));
                int r = Math.abs(radius.x - center.x);
                // 원점이 가려지거나 일부분이 가려진경우 해당지점의 범위를 효과적으로 구하기 힘들기 때문에
                // 비트맵 사이즈를 넘어가는 크기더라도 일단 관심영역으로 지정하고 추후에 표시할때 걸러냄
                int min_x = center.x-r; if(min_x < 0) min_x = 0;
                int max_x = center.x+r; if(max_x > width) max_x = width;
                int min_y = center.y-r; if(min_y < 0) min_y = 0;
                int max_y = center.y+r; if(max_y > height) max_y = height;

                // 관심영역으로 선정된 영역을 BFS를 이용하여 추정
                int w = max_x-min_x+1;
                int h = max_y-min_y+1;
                int[][] map = new int[w][h];
                Vector<Point> queue = new Vector<Point>();
                queue.add(new Point(center.x-min_x, center.y-min_y));
                map[center.x-min_x][center.y-min_y] = r;

                // BFS
                while(!queue.isEmpty()) {
                    Point point = queue.firstElement();
                    int x = point.x;
                    int y = point.y;
                    int bx = x+min_x;
                    int by = y+min_y;
                    if(map[x][y] > 0 && bx >= 0 && bx < width && by >=0 && by < height) {
                        // 벽에 의한 감소 적용
                        int d = 3;
                        if(mBitmap.getPixel(bx, by) == Color.BLACK) d = 8;

                        // Bresenham Algorithm 순서에 의한 BFS 적용 (원형으로 색칠)
                        int xk = 0;
                        int yk = 5;
                        int pk = -7; // 3 - 2*yk
                        int rx,ry;
                        while(xk<=yk) {
                            rx = x+xk; ry = y-yk;
                            if(rx >= 0 && rx < w && ry >= 0 && ry < h && (map[x][y] - map[rx][ry] > 10)) {
                                map[rx][ry] = map[x][y] - d;
                                queue.add(new Point(rx, ry));
                            }
                            rx = x-xk; ry = y-yk;
                            if(rx >= 0 && rx < w && ry >= 0 && ry < h && (map[x][y] - map[rx][ry] > 10)) {
                                map[rx][ry] = map[x][y] - d;
                                queue.add(new Point(rx, ry));
                            }
                            rx = x+xk; ry = y+yk;
                            if(rx >= 0 && rx < w && ry >= 0 && ry < h && (map[x][y] - map[rx][ry] > 10)) {
                                map[rx][ry] = map[x][y] - d;
                                queue.add(new Point(rx, ry));
                            }
                            rx = x-xk; ry = y+yk;
                            if(rx >= 0 && rx < w && ry >= 0 && ry < h && (map[x][y] - map[rx][ry] > 10)) {
                                map[rx][ry] = map[x][y] - d;
                                queue.add(new Point(rx, ry));
                            }
                            rx = x+yk; ry = y-xk;
                            if(rx >= 0 && rx < w && ry >= 0 && ry < h && (map[x][y] - map[rx][ry] > 10)) {
                                map[rx][ry] = map[x][y] - d;
                                queue.add(new Point(rx, ry));
                            }
                            rx = x+yk; ry = y+xk;
                            if(rx >= 0 && rx < w && ry >= 0 && ry < h && (map[x][y] - map[rx][ry] > 10)) {
                                map[rx][ry] = map[x][y] - d;
                                queue.add(new Point(rx, ry));
                            }
                            rx = x-yk; ry = y-xk;
                            if(rx >= 0 && rx < w && ry >= 0 && ry < h && (map[x][y] - map[rx][ry] > 10)) {
                                map[rx][ry] = map[x][y] - d;
                                queue.add(new Point(rx, ry));
                            }
                            rx = x-yk; ry = y+xk;
                            if(rx >= 0 && rx < w && ry >= 0 && ry < h && (map[x][y] - map[rx][ry] > 10)) {
                                map[rx][ry] = map[x][y] - d;
                                queue.add(new Point(rx, ry));
                            }

                            xk += 1;
                            if(pk < 0) pk += (xk*4)+6;
                            else {
                                yk -= 1;
                                pk += ((xk-yk)*4)+10;
                            }
                        }
                    }

                    queue.remove(0);
                }

                // 결과를 비트맵에 저장
                for(int x=0; x<w; x++) {
                    for(int y=0; y<h; y++) {
                        int bx = x+min_x;
                        int by = y+min_y;
                        // 비트맵에 표시가능한 것만 표시
                        if(bx >= 0 && bx < width && by >= 0 && by < height) {
                            int level = map[x][y]*255/r;
                            int color = mBitmap.getPixel(bx, by);
                            if(level < 100) {
                                //mBitmap.setPixel(bx, by, Color.argb(0,0,0,0));
                            }else if(level < 150) {
                                if(color != Color.argb(80,0,0,255) && color != Color.argb(80,0,255,0)
                                        && color != Color.argb(80,150,150,0) && color != Color.argb(80,200,100,0))
                                    mBitmap.setPixel(bx, by, Color.argb(80,255,0,0));
                            }else if(level < 170) {
                                if(color != Color.argb(80,0,0,255) && color != Color.argb(80,0,255,0)
                                        && color != Color.argb(80,150,150,0))
                                    mBitmap.setPixel(bx, by, Color.argb(80,200,100,0));
                            }else if(level < 190) {
                                if(color != Color.argb(80,0,0,255) && color != Color.argb(80,0,255,0))
                                    mBitmap.setPixel(bx, by, Color.argb(80,150,150,0));
                            }else if(level < 220) {
                                if(color != Color.argb(80,0,0,255))
                                    mBitmap.setPixel(bx, by, Color.argb(80,0,255,0));
                            }else {
                                mBitmap.setPixel(bx, by, Color.argb(80,0,0,255));
                            }
                        }
                    }
                }
            }

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

        CameraPosition position = intent.getParcelableExtra(getResources().getString(R.string.position));
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
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
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
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
    public void onClickBack(View view) {
        if(!mDrawPoints.isEmpty()) {
            mDrawPoints.remove(mDrawPoints.size()-1);
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
        mDeviceListView.setVisibility(View.VISIBLE);
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

        // 실제 선정된 기기를 통한 연산
        mDrawCanvas.simulate();
        mCurrentMode = mMode.RUN;
    }
    public void onClickExit(View view) {
        IS_MAP_MOVEABLE = false;
        mCurrentMode = mMode.EXIT;
        // Activity 전환
        finish();
    }

}
