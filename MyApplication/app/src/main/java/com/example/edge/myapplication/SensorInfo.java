package com.example.edge.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by park on 2015-11-03.
 */
public class SensorInfo implements Parcelable{

    private float acc[];
    private float accX[];
    private float accY[];
    private float accZ[];
    private int t = 0;

    public SensorInfo(){
        acc = new float[3];
        accX = new float[100];
        accY = new float[100];
        accZ = new float[100];
    }

    public SensorInfo(Parcel in){
        in.readFloatArray(accX);
        in.readFloatArray(accY);
        in.readFloatArray(accZ);
        t = in.readInt();
    }

    public static final Creator<SensorInfo> CREATOR = new Creator<SensorInfo>() {
        @Override
        public SensorInfo createFromParcel(Parcel in) {
            return new SensorInfo(in);
        }

        @Override
        public SensorInfo[] newArray(int size) {
            return new SensorInfo[size];
        }
    };

    public void setAccSensor(float x, float y, float z){
        this.accX[t] = x;
        this.accY[t] = y;
        this.accZ[t] = z;
        t++;
        if(t >= 100) t = 0;
    }

    public float[] getAccX(){
        return accX;
    }

    public float[] getAccY(){
        return accY;
    }

    public float[] getAccZ(){
        return accZ;
    }

    public float getAccSensor(int idx){
        return acc[idx];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloatArray(accX);
        dest.writeFloatArray(accY);
        dest.writeFloatArray(accZ);
        dest.writeInt(t);
    }
}
