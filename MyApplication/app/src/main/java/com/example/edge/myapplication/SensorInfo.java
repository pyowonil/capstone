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
public class SensorInfo implements Parcelable{

    private float acc[];
    private float accX[];
    private float accY[];
    private float accZ[];
    private float gyro[];
    private int t = 0;
    private final int size = 300;
    private float accG[];
    private float bias[];
    private float cnt = 0;
    private float svm = 0;
    float thres = 0;
    int step = 0;
    int stepCnt = 0;
    boolean isThres = false;
    float maxSVM = 0;
    float thresValue = 1.5f;
    float peakData[] = new float[6];
    int peakIndex = 0;

    public SensorInfo(){
        acc = new float[3];
        accX = new float[size];
        accY = new float[size];
        accZ = new float[size];
        gyro = new float[3];
        accG = new float[3];
        bias = new float[3];
        peakData[peakIndex] = 1.5f;
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

    long time;

    public void setTime(long time){
        this.time = time;
    }

    public double getTime(){
        return time*10e-4;
    }

    float[] reduc = new float[3];

    public void setAccSensor(float[] a){
        acc[0] = a[0];
        acc[1] = a[1];
        acc[2] = a[2];
        this.accX[t] = a[0];
        this.accY[t] = a[1];
        this.accZ[t] = a[2];
        t++;
        if(t >= size) t = 0;
    }

    public void setAccSensor(float x, float y, float z){
        if(cnt < 1000){
            reduc[0] += x;
            reduc[1] += y;
            reduc[2] += z;
            cnt++;
        }else{
            bias[0] = reduc[0]/cnt;
            bias[1] = reduc[1]/cnt;
            bias[2] = reduc[2]/cnt;
        }
        svm = (float)sqrt(pow(x,2) + pow(y,2) + pow(z,2))*20;
//        svm = (float)(pow(x,2) + pow(y,2) + pow(z,2));
        // acc[0]는 이동거리
//        Log.d("[센서] ", "SVM 값 " + svm);
        acc[0] = x;
        acc[1] = y;
        acc[2] = z;

//        this.accX[t] = x;
//        this.accY[t] = y;
//        this.accZ[t] = z;
//        t++;
//        if(t >= size) t = 0;

//        updatePeakData();
//        movingSVM(svm);
    }



    public void updatePeakData(){

    }

    float minThreshold = 20;
    float maxThreshold = 50;
    float Pn1 = minThreshold, Pn2 = minThreshold;
    float Mn1 = maxThreshold, Mn2 = maxThreshold;
    float K = 0;
    float C1 = 0.8f;
    float C2 = 0.8f;
    boolean th = false;

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

    EulerKalman ek = new EulerKalman();

    public void setGyro(float[] gyro){
        this.gyro[0] = gyro[0];
        this.gyro[1] = gyro[1];
        this.gyro[2] = gyro[2];
        ek.set(gyro[0],gyro[1],gyro[2],getTime(),acc[0],acc[1],acc[2]);
        ek.run();
        this.accX[t] = (float)ek.phi;
        this.accY[t] = (float)ek.theta;
        this.accZ[t] = (float)ek.psi;
        t++;
        if(t >= size) t = 0;
    }

    public float getGyro(int idx){
        return gyro[idx];
    }

    public int getStepCnt(){
        if(stepCnt == 1)
            return t;
        else
            return 0;
    }

    public int getStep(){
        return step;
    }

    public float getMaxSVM(){
        return maxSVM;
    }

    public float getThres(){
        return thresValue;
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

    public float getSVM(){
        return svm;
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
