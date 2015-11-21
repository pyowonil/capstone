package com.example.edge.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Created by park on 2015-11-03.
 */
public class SensorInfo {

    private float acc[][];
    private float gyro[];
    private float data[][];
    private int t = 0;
    private int t2 = 0;
    private final int size = 300;
    private float svm = 0;
    private int step = 0;
    private boolean isThres = false;
    private float peakData[] = new float[6];
    private int peakIndex = 0;
    private long time;

    float minThreshold = 20;
    float maxThreshold = 50;
    float Pn1 = minThreshold, Pn2 = minThreshold;
    float Mn1 = maxThreshold, Mn2 = maxThreshold;
    float K = 0;
    float C1 = 0.8f;
    float C2 = 0.8f;
    boolean th = false;

    public SensorInfo(){
        acc = new float[3][size];
        gyro = new float[3];
        peakData[peakIndex] = 1.5f;
        data = new float[3][size];
    }


    public void setTime(long time){
        this.time = time;
    }

    public double getTime(){
        return time*10e-4;
    }

    private double meter = 0;

    public double getMeter(){
        return meter;
    }

    public void setAccSensor(float[] a){
        this.acc[0][t] = a[0];
        this.acc[1][t] = a[1];
        this.acc[2][t] = a[2];


//        meter += a[0];
//        double dt = getTime();
//
//        meter += 0.5*a[0]*dt*dt;

        svm = (float)sqrt(pow(a[0],2) + pow(a[1],2) + pow(a[2],2))*20;
        movingSVM(svm);
        t++;
        if(t >= size) t = 0;
    }

    private void movingSVM(float svm){
        if(minThreshold <= svm){
            if(maxThreshold <= svm){
                if(!th) th = true;
                else{
                    if(Pn1 < svm) Pn1 = svm;
                }
            }else{
                if(th){
                    step++;
                    th = false;
                    Pn2 = Pn1;
                    Mn2 = Mn1;
                    if(Pn1 < Pn2){
                        K = Pn1;
                    }else{
                        K = Pn2;
                    }
                    float t = (Mn1+Mn2)/2;
                    minThreshold = t + (K-t)*C1;
                    maxThreshold = minThreshold + (float)sqrt(abs(K-minThreshold))*C2;
                    Pn1 = minThreshold;
                    Mn1 = maxThreshold;
                }
            }
        }else{
            if(Mn1 > svm) Mn1 = svm;
        }

    }

    public void setGyro(float[] gyro){
        this.gyro[0] = gyro[0];
        this.gyro[1] = gyro[1];
        this.gyro[2] = gyro[2];
    }

    public float getGyro(int idx){
        return gyro[idx];
    }

    public void setData(int idx, float[] data){
        this.data[0][idx] = data[0];
        this.data[1][idx] = data[1];
        this.data[2][idx] = data[2];

//        double dt = getTime();
//
//        meter += 0.5*data[0]*dt*dt;
        t2++;
        if(t2 >= size) t2 = 0;
    }

    public void setData(float[] data){
        this.data[0][t2] = data[0];
        this.data[1][t2] = data[1];
        this.data[2][t2] = data[2];

        t2++;
        if(t2 >= size) t2 = 0;
    }

    public float getData(int axis, int idx){
        return data[axis][idx];
    }

    public int getStep(){
        return step;
    }
    public float getSVM(){
        return svm;
    }

    public float getAccSensor(int axis, int idx){
        return acc[axis][idx];
    }

    public int getMaxSize(){
        return size;
    }

    public int getT(){
        return t;
    }

    public int getT2(){
        return t2;
    }

}
