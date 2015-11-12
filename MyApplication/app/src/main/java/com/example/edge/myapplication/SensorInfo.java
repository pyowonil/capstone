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
    private final int size = 300;
    private float accG[];
    private float bias[];
    private float cnt = 0;

    public SensorInfo(){
        acc = new float[3];
        accX = new float[size];
        accY = new float[size];
        accZ = new float[size];
        accG = new float[3];
        bias = new float[3];
    }

    public SensorInfo(Parcel in){
        in.readFloatArray(accX);
        in.readFloatArray(accY);
        in.readFloatArray(accZ);
        in.readFloatArray(accG);
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
        acc[0] = x;
        acc[1] = y;
        acc[2] = z;
        this.accX[t] = x;
        this.accY[t] = y;
        this.accZ[t] = z;
        t++;
        if(t >= size) t = 0;
    }

    public void setAccG(float x, float y, float z){
        accG[0] = x;
        accG[1] = y;
        accG[2] = z;
        if(cnt < 1000){
            bias[0] += x;
            bias[1] += y;
            bias[2] += z;
            cnt++;
        }
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

    public float getAccG(int idx){
        return accG[idx];
    }

    public float getBias(int idx){
        if(cnt == 1000){
            return bias[idx]/cnt;
        }else{
            return cnt;
        }
    }

    public int getIndex(){
        return t;
    }

    public int getMaxSize(){
        return size;
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
