package com.example.edge.myapplication;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class SensorService extends Service implements SensorEventListener{

    SensorManager sensorManager;
    SensorEventListener accListener;
    Sensor accSensor;
    float acc[];
    SensorInfo sensorInfo;

    @Override
    public void onCreate(){
        super.onCreate();
        // Get Sensor Information
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        // Get Accelerometer
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        acc = new float[3];
        sensorInfo = new SensorInfo();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        sensorManager.registerListener(this,   accSensor, SensorManager.SENSOR_DELAY_NORMAL); // 가속도
        return super.onStartCommand(intent,flags,startId);
//        sensorManager.registerListener(this,   accSensor, SensorManager.SENSOR_DELAY_NORMAL); // 가속도
//        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        sensorManager.unregisterListener(this);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this){
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    acc[0] = event.values[0];
                    acc[1] = event.values[1];
                    acc[2] = event.values[2];
                    sensorInfo.setAccSensor(acc[0], acc[1], acc[2]);
                    //Intent sensorIntent = new Intent(SensorService.this,MainActivity.class);
                    Intent sensorIntent = new Intent("android.intent.action.MAIN");
                    sensorIntent.putExtra("sensorData",sensorInfo);
//                    sensorIntent.putExtra("sensorData",acc);
                    sendBroadcast(sensorIntent);
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {


    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
