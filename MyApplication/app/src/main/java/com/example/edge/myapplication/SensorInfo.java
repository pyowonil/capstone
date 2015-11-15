package com.example.edge.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
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

    public SensorInfo(){
        acc = new float[3];
        accX = new float[size];
        accY = new float[size];
        accZ = new float[size];
        gyro = new float[3];
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

    long time;

    public void setTime(long time){
        this.time = time;
    }

    public double getTime(){
        return time*10e-4;
    }

    float[] reduc = new float[3];

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
        svm = (float)sqrt(pow(x,2) + pow(y,2) + pow(z,2));

        // acc[0]는 이동거리
        acc[0] += x;
        if(Math.abs(acc[0]) > 10e17) acc[0] /= 10e18;
        acc[1] = y;
        acc[2] = z;
        this.accX[t] = x;
        this.accY[t] = y;
        this.accZ[t] = z;
        t++;
        if(t >= size) t = 0;

//        movingSVM(svm);
    }

    public void setGyro(float[] gyro){
        this.gyro[0] = gyro[0];
        this.gyro[1] = gyro[1];
        this.gyro[2] = gyro[2];
    }

    public float getGyro(int idx){
        return gyro[idx];
    }

    float stdMin = 99;
    float stdMax = -99;
    float minSVM = 99;
    float maxSVM = -99;
    float prevMinSVM = 99;
    float prevMaxSVM = -99;

    float minThres = 99;
    float maxThres = -99;
    float thres = 0;
    float meanThres = 0;
    float thCnt = 0;
    int step = 0;
    int stepCnt = 0;

    public int getStepCnt(){
        if(stepCnt == 1)
            return t;
        else
            return 0;
    }

    public int getStep(){
        return step;
    }

    private void movingSVM(float svm){
        if(stdMin == 99){
            stdMin = svm;
            stdMax = svm;
        }else{
            if(stdMin < svm){
                if(minSVM == 99){
                    minSVM = stdMin;
                }else{
                    prevMinSVM = minSVM;
                    minSVM = stdMin;
                }
                stdMin = svm;
            }else{
                stdMin = svm;
            }

            if(stdMax < svm){
                stdMax = svm;
            }else{
                if(maxSVM == -99){
                    maxSVM = stdMax;
                }else{
                    prevMaxSVM = maxSVM;
                    maxSVM = stdMax;
                }
                stdMax = svm;
            }
        }

        if(maxSVM != -99 && minSVM != 99){
            thres = (maxSVM+minSVM)/2;

            if(4 < svm && svm < 7 && stepCnt == 0){
                stepCnt = 1;
                step++;
            }else if(4  > svm && stepCnt == 1){
                stepCnt = 0;
            }

            meanThres += thres;
            thCnt++;

            if(minThres == 99){
                minThres = thres;
            }else{
                if(minThres > thres) minThres = thres;
            }
            if(maxThres == -99){
                maxThres = thres;
            }else{
                if(maxThres < thres) maxThres = thres;
            }
        }
    }

    public float getMaxThres(){
        return maxThres;
    }

    public float getMinThres(){
        return minThres;
    }

    public float getThres(){
        return thres;
    }

    public float getMeanThres(){
        return meanThres/thCnt;
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
