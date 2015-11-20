package com.example.edge.myapplication;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import static java.lang.Math.*;
import Jama.Matrix;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    Messenger mServiceMessenger = null;
    boolean mIsBound;
    TextView mCallbackText, acctext;
    final String TAG = "[클라이언트]";

    private double dt = 1.0/100.0;
    private double processNoiseStdev = 3;
    private double measurementNoiseStdev = 5;
    double m = 0;
    Random jerk = new Random();
    Random sensorNoise = new Random();
    private KalmanFilter KF;

    LinearLayout linear;
    SensorInfo sensorInfo;

    // GPS ---------------------------------
    // gps object
    private GpsInfo m_gps;
    // button
    private Button m_btn_gpsinfo;
    // textview
    private TextView m_txtv_gpsinfo;
    // --------------------------------- GPS


    class ActivityHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case SensorService.MSG_SET_VALUE:
//                    Log.d(TAG, " 서비스로부터 값을 전달 받음");
                    sensorInfo = (SensorInfo)msg.obj;
                    if(sensorInfo != null){
//                        Log.d(TAG, " 가속도 값이 있음");
                        acctext.setText("x : " + sensorInfo.getAccSensor(0) + "\ny : " + sensorInfo.getAccSensor(1) + "\nz : " + sensorInfo.getAccSensor(2));
//                        mCallbackText.setText("이동거리(m) : " + sensorInfo.getAccSensor(0) + "\n이동속도(m/s) : " + sensorInfo.getAccSensor(1)
//                                + "\nx축 가속도(m/s) : " + sensorInfo.getAccSensor(2) + "\n시간(sec) : " + sensorInfo.getTime());
//                        mCallbackText.setText("Pn1 : " + sensorInfo.Pn1 + "\nPn2 : " + sensorInfo.Pn2
//                                + "\nMn1 : " + sensorInfo.Mn1 + "\nMn2 : " + sensorInfo.Mn2 + "\nSTEP : " + sensorInfo.getStep());
                        mCallbackText.setText("PHI : " + sensorInfo.ek.phi + "\nTHETA : " + sensorInfo.ek.theta + "\nPSI : " + sensorInfo.ek.psi);
                    }else{
//                        Log.d(TAG, " 가속도 값이 없음");
                        acctext.setText("not value");
                    }
                    try {
                        mServiceMessenger.send(Message.obtain(null, SensorService.MSG_SET_VALUE));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mAcitivityMessenger = new Messenger(new ActivityHandler());

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG," 서비스 연결완료");
            mServiceMessenger = new Messenger(service);
            mCallbackText.setText("Attached");

            try{
                Log.d(TAG," 서비스로 메시지 전송 시도");
                Message msg = Message.obtain(null,SensorService.MSG_REGISTER_CLIENT);
                msg.replyTo = mAcitivityMessenger;
                mServiceMessenger.send(msg);

                msg = Message.obtain(null, SensorService.MSG_SET_VALUE,this.hashCode(),0);
                mServiceMessenger.send(msg);
            }catch(RemoteException e){

            }
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
            mCallbackText.setText("Disconnected");
            Toast.makeText(MainActivity.this,"Disconnected",Toast.LENGTH_SHORT).show();
        }
    };

    private final View.OnClickListener mBindListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG," 연결 요청");
            doBindService();
        }
    };

    private final View.OnClickListener mUnBindListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG," 연결해제 요청");
            doUnbindService();
        }
    };

    void doBindService(){
        mIsBound = bindService(new Intent(MainActivity.this,SensorService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, " 연결 요청");
        mCallbackText.setText("Binding");
    }

    void doUnbindService(){
        if(mIsBound){
            Message msg = Message.obtain(null,SensorService.MSG_UNREGISTER_CLIENT);
            try{
                mServiceMessenger.send(msg);
            }catch(RemoteException e){

            }
            unbindService(mConnection);
            mIsBound = false;
            mCallbackText.setText("Unbinding");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linear = (LinearLayout)findViewById(R.id.linear);
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.linear);
        acctext = (TextView)findViewById(R.id.acctext);
        acctext.setText("x : " + 0 + "\ny : " + 0 + "\nz : " + 0);
        mCallbackText = (TextView)findViewById(R.id.mCallbackText);
        Log.d(TAG, " 생성");
        doBindService();
        SensorView sv = new SensorView(this);
        linearLayout.addView(sv);

        // GPS ---------------------------------
        // set gps object
        m_gps = new GpsInfo(MainActivity.this);
        m_btn_gpsinfo = (Button)findViewById(R.id.btn_gpsinfo);
        m_txtv_gpsinfo = (TextView)findViewById(R.id.gpsinfo);

        String gpsinfo = "Latitude : \nLongitude : ";
        m_txtv_gpsinfo.setText(gpsinfo);

        m_btn_gpsinfo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // gps 사용 유무 가져오기
                if(m_gps.isGetLocation()) {
                    double latitude, longitude;
                    latitude = m_gps.getLatitude();
                    longitude = m_gps.getLongitude();
                    String gpsinfo = "Latitude : " + Double.toString(latitude) + "\nLongitude : " + Double.toString(longitude);
                    m_txtv_gpsinfo.setText(gpsinfo);
                } else {
                    m_gps.showSettingsAlert();
                    String gpsinfo = "Latitude : " + "Error" + "\nLongitude : " + "Error";
                    m_txtv_gpsinfo.setText(gpsinfo);
                }
            }
        });
        // --------------------------------- GPS


    }

    public float simpson(float[]y, float a, float b) {
        return simpson(y, (b-a)/(y.length-1));
    }
    public float simpson(float[] y, float dx) {
        assert(y.length > 2 && y.length % 2 == 1);
        int n = y.length;
        float result = y[0] + y[n-1];
        float oddOrders = 0, evenOrders = -y[0];
        for(int i=1; i<n-1; i+=2) {
            oddOrders += y[i];
            evenOrders += y[i-1];
        }
        oddOrders *= 4;
        evenOrders *= 2;
        result += oddOrders + evenOrders;
        result = (float) ((result * dx)/3.0);

        return result;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        doUnbindService();
    }

    // temporary test class
    class SensorView extends View{

        float x = 0;
        float y = 0;
        private int t = 0;
        float y2 = 0;
        float q = 1;
        private Paint paint;
        private float accX[], accY[], accZ[];
        float intvls = 0;
        int maxSize = 0;

        public SensorView(Context context){
            super(context);
            paint = new Paint();
        }

        private void drawAcc(Canvas canvas, float accX[], float accY[], float accZ[]) {
            paint.setColor(Color.GRAY);
            paint.setStrokeWidth(5);
            canvas.drawLine(0, 0, 0, y, paint);
            canvas.drawLine(0, y2, x, y2, paint);
            canvas.drawLine(0, y2-20, x, y2-20, paint);
            canvas.drawLine(0, y2-50, x, y2-50, paint);
            canvas.drawLine(0, y2*3, x, y2*3, paint);
            canvas.drawLine(0, y2 * 5, x, y2 * 5, paint);
            paint.setStrokeWidth(5);

            for(int i=0; i<maxSize; i++) {
                int m = (t - i);
                if (m < 0) m += maxSize;
                int nm = m + 1;
                if (nm >= maxSize) nm -= maxSize;
                paint.setColor(Color.RED);
                canvas.drawLine((i + 1) * intvls, y2 -(accX[m] * q), i * intvls, y2 -(accX[nm] * q), paint);
                paint.setColor(Color.BLUE);
                canvas.drawLine((i + 1) * intvls, y2 * 3 -(accY[m] * q), i * intvls, y2 * 3 -(accY[nm] * q), paint);
                paint.setColor(Color.GREEN);
                canvas.drawLine((i + 1) * intvls, y2 * 5 -(accZ[m] * q), i * intvls, y2 * 5 -(accZ[nm] * q), paint);
            }
        }

        @Override
        protected void onDraw(Canvas canvas){
            synchronized (this) {
                super.onDraw(canvas);
                paint.setAntiAlias(true);
                if (x == 0) {
                    if (sensorInfo != null) {
                        maxSize = sensorInfo.getMaxSize();
                        x = canvas.getWidth();
                        y = canvas.getHeight();
                        y2 = y / 6;
                        intvls = x / maxSize;
                    }
                }
                if (sensorInfo != null) {
                    if (maxSize == 0) {
                        maxSize = sensorInfo.getMaxSize();
                    }
                    accX = sensorInfo.getAccX();
                    accY = sensorInfo.getAccY();
                    accZ = sensorInfo.getAccZ();
                    t = sensorInfo.getIndex();
                    drawAcc(canvas, accX, accY, accZ);
                } else {       }
                invalidate();
            }
        }
    }
}
