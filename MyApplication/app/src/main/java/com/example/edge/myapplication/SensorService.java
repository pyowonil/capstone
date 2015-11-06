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
    Sensor accSensor;
    float acc[];
    SensorInfo sensorInfo;

    // create handler class
    class ServiceHandler extends Handler{
        @Override
        public void handleMessage(Message msg){

            switch (msg.what){
                case MSG_REGISTER_CLIENT:
                    Log.d(TAG, " 클라이언트 등록");
                    mAcitivityMessenger = msg.replyTo;
                    mText = "MSG_REGISTER_CLIENT";
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.d(TAG," 클라이언트 해제");
                    mText = "MSG_UNREGISTER_CLIENT";
                    mAcitivityMessenger = null;
                    break;
                case MSG_SET_VALUE:
                    Log.d(TAG," 클라이언트로 값 전달");
                    mValue = msg.arg1;
                    mText = "MSG_SET_VALUE : " + Integer.toString(mValue);
                    if(mAcitivityMessenger == null) break;
                    try{
                        Message msg2 = Message.obtain(null, MSG_SET_VALUE, mValue, 0);
//                        Bundle bundle = new Bundle();
//                        bundle.putParcelable("sensor",sensorInfo);
//                        msg2.setData(bundle);
                        msg2.obj = sensorInfo;
                        mAcitivityMessenger.send(msg2);
                    }catch(RemoteException e){

                    }
                    break;
                default:
                    super.handleMessage(msg);
                    mText = "Default";
            }
            //Toast.makeText(SensorService.this, mText, Toast.LENGTH_SHORT).show();
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
        acc = new float[3];
        sensorInfo = new SensorInfo();
        Log.d(TAG, " 생성");
        Log.d(TAG," 센서 등록");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        sensorManager.unregisterListener(this);
        Log.d(TAG, " 센서 해제");
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
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {


    }

    @Override
    public IBinder onBind(Intent intent){
        Log.d(TAG, " 연결 요청 받음");
        Toast.makeText(this,"BINDING..",Toast.LENGTH_SHORT).show();
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL); // 가속도
        return mServiceMessenger.getBinder();
    }
}
