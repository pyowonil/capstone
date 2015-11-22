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

import Jama.Matrix;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Created by park on 2015-11-03.
 */
public class SensorInfo {

    private float acc[][];
    private float accData[];
    private float gyro[];
    private float data[][];
    private int t = 0;
    private int t2 = 0;
    private final int size = 300;
    public float svm = 0;
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
    float C1 = -0.5f;
    float C2 = 0.5f;
    boolean th = false;
    private int peakIdx = 0;
    private float[] peak = new float[10];
    private float thres = 20;

    public SensorInfo(){
        acc = new float[3][size];
        accData = new float[3];
        gyro = new float[3];
        peakData[peakIndex] = 1.5f;
        data = new float[3][size];
        for(int i=0;i<10;i++) peak[i] = 20;
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

        accData = a;
//        meter += a[0];
//        double dt = getTime();
//        meter += 0.5*a[2]*dt*dt;

//        svm = (float)sqrt(pow(a[0],2) + pow(a[1],2) + pow(a[2],2))*20;
//        movingSVM(svm);
        t++;
        if(t >= size) t = 0;
    }

    private float tmp = thres;

    public float getThres(){
        return thres;
    }

    private void movingSVM(float svm){
        if(20 <= svm){
            if(!th) th=true;
            else{
                if(tmp < svm) tmp = svm;
            }
        }else{
            if(th){
                th = false;
                peak[peakIdx] = tmp;
                peakIdx++;
                if(peakIdx >= 10) peakIdx = 0;
                float tmp2 = 0;
                for(int i=0; i<10; i++) tmp2+=peak[i];
                thres = thres*0.7f + (tmp2*0.1f)*0.3f;
                tmp = thres;
                step++;
            }
        }

//        if(minThreshold <= svm){
//            if(maxThreshold <= svm){
//                if(!th) th = true;
//                else{
//                    if(Pn1 < svm){
//                        Pn1 = svm;
//                    }
//                }
//            }else{
//                if(th){
//                    step++;
//                    th = false;
//                    Pn2 = Pn1;
//                    Mn2 = Mn1;
//                    if(Pn1 < Pn2){
//                        K = Pn1;
//                    }else{
//                        K = Pn2;
//                    }
//                    float t = (Mn1+Mn2)/2;
//                    minThreshold = t + (K-t)*C1;
//                    maxThreshold = minThreshold + (float)sqrt(abs(K-minThreshold))*C2;
//                    Pn1 = 50;
//                    Mn1 = 20;
//                }
//            }
//        }else{
//            if(Mn1 > svm) Mn1 = svm;
//        }

    }

    AttitudeKF akf = new AttitudeKF();
    PosVelKF pkf = new PosVelKF();
    double tm_x, tm_y;
    double rpy[] = new double[3];
    double a[][] = new double[3][];
    double v[][] = new double[3][];
    double p[][] = new double[3][];
    double P[][] = new double[6][6];
    double Q[][] = new double[3][3];

    public void setGyro(float[] gyro){
        this.gyro[0] = gyro[0];
        this.gyro[1] = gyro[1];
        this.gyro[2] = gyro[2];

        pkf.predict(akf.get_X(), gyro, accData, getTime());
        pkf.update(akf.get_X());
        akf.predict(gyro, getTime());
        akf.update(gyro, accData);
        akf.GetRPY(rpy[0],rpy[1],rpy[2]);
        a = pkf.get_a().getArray();
        Matrix x = pkf.get_X();
        v = x.getMatrix(0,2,0,0).getArray();
        p = x.getMatrix(3,5,0,0).getArray();
        Matrix _P = pkf.get_P();
        Matrix _Q = pkf.get_P();
        // For Debugging
        for (int i=0; i<6; ++i) {
            for (int j=0; j<6; ++j) {
                P[i][j] = _P.get(i, j);
            }
        }
        for (int i=0; i<3; ++i) {
            for (int j=0; j<3; ++j) {
                Q[i][j] = _Q.get(i, j);
            }
        }

        // 우리나라 TM(Transverse Mercator) 좌표계 원점들의 경위도 값
        // 중부 원점: 38, 127
        // x-축 : 남북 방향, 북쪽이 +
        // y-축 : 동서 방향, 동쪽이 +
        Bessel2TM (altitude, longitude,latitude, 127, 38);
        tm_y = -tm_y;

        double yaw = -DEG2RAD*bearing;
        double vel = speed*1000./(60.*60.);

        akf.update(yaw, vel);
        pkf.update(akf.get_X(), tm_x, tm_y, altitude);
        pkf.update(akf.get_X(), vel, 0.01);

        tm_x += 200000;
        tm_y += 500000;

        if(px == 0){
            px = tm_x;
        }
        if(py == 0){
            py = tm_y;
        }

        distance = Math.sqrt(Math.pow((tm_x-px),2) + Math.pow((tm_y-py),2));
        XDistance = tm_x-px;
        YDistance = tm_y-py;
        px = tm_x;
        py = tm_y;
        dx[t2] = (float)XDistance;
        dy[t2] = (float)YDistance;
        t2++;
        if(t2 >= 300) t2 = 0;
    }

    public float getDx(int i){
        return dx[i];
    }

    public float getDy(int i){
        return dy[i];
    }

    private float[] dx = new float[300];
    private float[] dy = new float[300];

    public float getXDistance(){
        return (float)XDistance;
    }
    public float getYDistance(){
        return (float)YDistance;
    }

    public void setXDistance(float XDistance){
        this.XDistance = XDistance;
    }

    public void setYDistance(float YDistance){
        this.YDistance = YDistance;
    }

    public double getDistance(){
        return distance;
    }

    private double XDistance = 0;
    private double YDistance = 0;
    private double distance = 0;
    private double px = 0;
    private double py = 0;

    public double getTm_x(){
        return tm_x;
    }

    public double getTm_y(){
        return tm_y;
    }

    public double getSpeed(){
        return speed;
    }

    public double getAltitude(){
        return altitude;
    }

    public double getLongitude(){
        return longitude;
    }

    public double getLatitude(){
        return latitude;
    }

    public double getBearing(){
        return bearing;
    }

    private double altitude = 0;
    private double longitude = 127;
    private double latitude = 38;
    private double speed = 0;
    private double bearing = 0;

    public void setGPS(double latitude, double longitude, double altitude, double speed, double bearing){
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.bearing = bearing;
    }

    private double PI = 3.14159265358979323846;
    private double DEG2RAD = (PI/180);
    private double RAD2DEG = (180/PI);

    public void Bessel2TM (double alt, double lon, double lat, double lonOrg, double latOrg) {
        double R0 = 6378137.0;	// 지구 장축 길이
        double e = 0.081819191;	// 이심률

        double phi = Math.toRadians(lat);
        double den = sqrt(1 - e*e*Math.sin(phi)*Math.sin(phi));
        double RN = R0*(1 - e*e)/(den*den*den);
        double RE = R0/den;

        // 우리나라 TM(Transverse Mercator) 좌표계 원점들의 경위도 값
        // x-축 : 남북 방향, 북쪽이 +
        // y-축 : 동서 방향, 동쪽이 +
        tm_x = (RN + alt)*Math.toRadians(lat - latOrg);
        tm_y = (RE + alt)*Math.toRadians(lon - lonOrg)*Math.cos(phi);
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
