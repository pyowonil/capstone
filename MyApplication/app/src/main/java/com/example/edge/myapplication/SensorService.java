package com.example.edge.myapplication;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.util.Random;

import Jama.Matrix;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class SensorService extends Service implements SensorEventListener{

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_VALUE = 3;
    static Messenger mAcitivityMessenger = null;
    int mValue = 0;
    final String TAG = "[서비스]";
    int i = 110;
    static String mText = null;
    SensorManager sensorManager;
    Sensor accSensor, gyroSensor, mgnSensor;
    float[] acc;
    float[] gyro;
    SensorInfo sensorInfo;
    private double dt = 1.0/1000.0;
    private double processNoiseStdev = 0.1;//3;
    private double measurementNoiseStdev = 2.8;//5;
    double m = 0;
    Random jerk = new Random();
    Random sensorNoise = new Random();
    private KalmanFilter KF;
    Filter filter=new Filter();
    double ax = 0;
    double vx = 0;
    double tx = 0;
    float alpha = 0.8f;
    float gx = 0;
    float gy = 0;
    float gz = 0;

    float thres = 2.5f;
    float width = 0.5f;
    float std = -99.f;
    float std2 = -99.f;
    float meter = 0.f;

    EKF ekf = new EKF();
    EulerAngles m_euler_angles = new EulerAngles();
    //EulerKalman m_euler_kalman = new EulerKalman();

    GpsInfo gps;

    // create handler class
    class ServiceHandler extends Handler{
        @Override
        public void handleMessage(Message msg){

            switch (msg.what){
                case MSG_REGISTER_CLIENT:
                    Log.d(TAG, " 클라이언트 등록");
                    mAcitivityMessenger = msg.replyTo;
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.d(TAG," 클라이언트 해제");
                    mAcitivityMessenger = null;
                    break;
                case MSG_SET_VALUE:
//                    Log.d(TAG," 클라이언트로 값 전달");
                    if(mAcitivityMessenger == null) break;

                    try{
                        if(gps == null){
                            gps = (GpsInfo)msg.obj;
                        }
                        Message msg2 = Message.obtain(null, MSG_SET_VALUE, mValue, 0);
                        msg2.obj = sensorInfo;
                        mAcitivityMessenger.send(msg2);
                    }catch(RemoteException e){

                    }
                    break;
                default:
                    super.handleMessage(msg);
                    mText = "Default";
            }
        }
    }

    // using handler object -> create messenger object
    final Messenger mServiceMessenger = new Messenger(new ServiceHandler());

    @Override
    public void onCreate(){
        super.onCreate();
        // Get Sensor Information
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        // Get Accelerometer
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mgnSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        acc = new float[3];
        gyro = new float[3];
        sensorInfo = new SensorInfo();
        Log.d(TAG, " 생성");
        Log.d(TAG," 센서 등록");
        ekf.set_dt(dt);
        KF = KalmanFilter.buildKF(dt, pow(processNoiseStdev, 2) / 2, pow(measurementNoiseStdev, 2));
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        sensorManager.unregisterListener(this);
        Log.d(TAG, " 센서 해제");
    }

    long stime = 0;
    long etime = 0;
    GyroToEulerAngle gea = new GyroToEulerAngle();

    float[] angles = new float[3];


    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this){
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    if(stime == 0) stime = System.currentTimeMillis();
                    acc[0] = event.values[0];
                    acc[1] = event.values[1];
                    acc[2] = event.values[2];
//                    etime = System.currentTimeMillis();
//                    sensorInfo.setTime(etime-stime);
//                    stime = etime;
//                    double dt = sensorInfo.getTime();
                    // EulerAngles test ------------------------------
                    m_euler_angles.setF(event.values[0], event.values[1], event.values[2]);
                    //m_euler_kalman.setAccel(event.values[0], event.values[1], event.values[2]);
                    // ------------------------------ EulerAngles test
//                    sensorInfo.setAccSensor(m_euler_angles.getFn(event.values));
                    // 중력가속도 제거
                    gx = alpha*gx + (1-alpha)*acc[0];
                    gy = alpha*gy + (1-alpha)*acc[1];
                    gz = alpha*gz + (1-alpha)*acc[2];
                    acc[0] = acc[0] - gx;
                    acc[1] = acc[1] - gy;
                    acc[2] = acc[2] - gz;

                    // EulerAngles test ------------------------------
//                    acc = m_euler_angles.getFn(acc);
//                    acc[0] = (float)(m_euler_angles.getPhi() * 180/31.41592);
//                    acc[1] = (float)(m_euler_angles.getTheta() * 180/31.41592);
//                    acc[2] = (float)(m_euler_angles.getPsi() * 180/31.41592);
                    //acc[0] = (float)(m_euler_kalman.getPhi() * 36/31.41592);
                    //acc[1] = (float)(m_euler_kalman.getTheta() * 36/31.41592);
                    //acc[2] = (float)(m_euler_kalman.getPsi() * 36/31.41592);
                    // ------------------------------ EulerAngles test

                    // HSR 적용
                    acc = filter.HSR(acc);
                    sensorInfo.setAccSensor(acc);

                    // ------------------ Kalman Filter 적용 부 ------------------
                    // 보정 안한
//                    ax = (double) event.values[0] * processNoiseStdev;
//                    acc = m_euler_angles.getFn(acc);
//                    sensorInfo.setData(acc);
//                    sensorInfo.setAccSensor(acc);
//                    acc = m_euler_angles.getFn(acc);
//                    sensorInfo.setAccSensor(acc);

//                    ax = (double) acc[0] * processNoiseStdev;
//                    vx = ax * dt;
//                    tx = dt * vx + 0.5 * pow(dt, 2) * ax;
//                    KF.setX(new Matrix(new double[][]{{tx}, {vx}, {ax}}));
//                    m = tx + sensorNoise.nextGaussian() * measurementNoiseStdev;
//                    // dt값 변동을 위한 update함수
//                    KF.update(dt);
//                    KF.predict();
//                    KF.correct(new Matrix(new double[][]{{tx}}));
//                    float[] f = new float[3];
//                    f[0] = (float)KF.getX().get(0,0); f[1] = (float)KF.getX().get(1,0); f[2] = (float)KF.getX().get(2,0);
//                    sensorInfo.setAccSensor(f);
                    // ----------------- Kalman Filter 적용 부 -------------------

//                    sensorInfo.setAccSensor(acc[0], acc[1], acc[2]);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    etime = System.currentTimeMillis();
                    sensorInfo.setTime(etime-stime);
                    stime = etime;
                    if(gps!=null){
                        sensorInfo.setGPS(gps.getLatitude(),gps.getLongitude(),gps.getAltitude(),gps.getSpeed(),gps.getBearing());
                    }
                    gyro = filter.HDR(event.values);
                    sensorInfo.setGyro(gyro);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    // EulerAngles test ------------------------------
//                    m_euler_angles.setH(event.values[0], event.values[1], event.values[2]);
//                    sensorInfo.setData(m_euler_angles.getFn());
                    //m_euler_kalman.setMagnetic(event.values[0], event.values[1], event.values[2]);
                    // ------------------------------ EulerAngles test
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {    }

    @Override
    public IBinder onBind(Intent intent){
        Log.d(TAG, " 연결 요청 받음");
        Toast.makeText(this,"BINDING..",Toast.LENGTH_SHORT).show();
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL); // 가속도
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mgnSensor, SensorManager.SENSOR_DELAY_NORMAL);
        return mServiceMessenger.getBinder();
    }
}
