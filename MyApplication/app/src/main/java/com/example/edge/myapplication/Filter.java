package com.example.edge.myapplication;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Created by park on 2015-11-12.
 */
public class Filter {

    private float scale = 1.0f; // 가속도 스케일 조정 값
    private float HSR_ic = 1.0001f;
    private float HDR_ic = 0.001f;
    private float HDR_I[] = new float[3];
    private float threshold = 15;
    private float T = 0.005f; // 샘플링 주기
    private float tau = 0.01f; // 1차 Low-pass Filter의 시상수(time constant)
    private boolean isInitLow;
    private boolean isInitDeLag;
    private float[] lowGyro;
    private float[] delagGyro;
    private float[] tmpDelagGyro;
    private boolean[] isInitRepetition;
    private float[] repetition;
    private float[] tmpReGyro;
    private float C1 = 0.7839f; // 반복 감쇠기 특성 값
    private float C2 = 1.2161f; // 반복 감쇠기 특성 값
    private float turnThresh = 0.5608f; // 강회전 문턱 값

    public Filter(){
        isInitLow = false;
        isInitDeLag = false;
        isInitRepetition = new boolean[3];

        lowGyro = new float[3];
        delagGyro = new float[3];
        tmpDelagGyro = new float[3];
        repetition = new float[3];
        tmpReGyro = new float[3];

        for(int i=0; i<3; i++){
            isInitRepetition[i] = false;
            repetition[i] = 1;
        }
    }

    // Heuristic Scale Regulation
    // 가속도 센서로 속도와 위치 계산 시 미소한 scale factor의 변화로
    // 중력 성분이 제대로 제거 되지 않아 누적 오차가 발생하는데
    // 이를 최소화하기 위해 scale factor를 조절
    public float[] HSR(float[] acc){
        // 정지 상태에서의 3축 가속도 평균 오차 제거 (1000개 데이터 샘플)
        acc[0] -= -1.8461699e-4;
        acc[1] -= -5.564482e-5;
        acc[2] -= 0.009868071;
        // ||a|| = sqrt(x^2 + y^2 + z^2)
        double ta =sqrt( pow(acc[0],2) + pow(acc[1],2) + pow(acc[2],2));

        // ||a|| 값에 따라 scale factor 조절
        // ic값은 1보다 미소하게 큰 값 사용
        if(ta > 1){
            scale = scale*HSR_ic;
        }else{
            scale = scale/HSR_ic;
        }
        acc[0] = (acc[0] * scale);
        acc[1] = (acc[1] * scale);
        acc[2] = (acc[2] * scale);

        return acc;
    }

    // Heuristic Drift Reduction
    //
    // Binary I-Controller Function는 작은 오차에는 민감하게
    // 큰 오차에서는 둔감하게 반응해야 한다.
    public float[] HDR(float[] gyro){
        // 정지 상태의 자이로 센서 평균 오차 제거 (10000 샘플)
        gyro[0] += (HDR_I[0] - 0.010702647);
        gyro[1] += (HDR_I[1] + 0.014681378);
        gyro[2] += (HDR_I[2] + 0.007977725);

        // Binary I-Controller 부분
        if( (-threshold < gyro[0] && gyro[0] < threshold) &&
                (-threshold < gyro[1] && gyro[1] < threshold) &&
                (-threshold < gyro[2] && gyro[2] < threshold)){

            HDR_I[0] -= SIGN(gyro[0])*HDR_ic;
            HDR_I[1] -= SIGN(gyro[1])*HDR_ic;
            HDR_I[2] -= SIGN(gyro[2])*HDR_ic;
        }

        return gyro;
    }

    public float[] EnhancedHDR(float[] gyro){

        gyro = LowPassFilter(gyro);

        gyro[0] += HDR_I[0];
        gyro[1] += HDR_I[1];
        gyro[2] += HDR_I[2];

        // Binary I-Controller 부분
        if( (-threshold < gyro[0] && gyro[0] < threshold) &&
                (-threshold < gyro[1] && gyro[1] < threshold) &&
                (-threshold < gyro[2] && gyro[2] < threshold)){

            HDR_I[0] -= SIGN(gyro[0])*HDR_ic*TurnSwitch(gyro, 0)*RepetitionAttenuator(gyro, 0);
            HDR_I[1] -= SIGN(gyro[1])*HDR_ic*TurnSwitch(gyro, 1)*RepetitionAttenuator(gyro, 1);;
            HDR_I[2] -= SIGN(gyro[2])*HDR_ic*TurnSwitch(gyro, 2)*RepetitionAttenuator(gyro, 2);;
        }

        return Delagging(gyro);
    }

    private int SIGN(float w){
        if (w > 0) return 1;
        if (w < 0) return -1;
        return 0;
    }

    // 강회전이 발생하엿을 경우 큰 값의 각속도가 측정되는데,
    // 이 때 Binary I-Controller의 동작을 일시정지
    private int TurnSwitch(float[] gyro, int idx){
        if( -turnThresh < gyro[idx] && gyro[idx] < turnThresh )
            return 1;
        else
            return 0;
    }

    // 반복 감쇠기(Repetition Attenuator)
    // 각속도의 부호 변동을 파악하여 각속도가 일정하게 유지하는 동안
    // 고정 증분값 ic를 점차적으로 줄임
    private float RepetitionAttenuator(float[] gyro, int idx){
        if(!isInitRepetition[idx]){
            tmpReGyro[idx] = gyro[idx];
            isInitRepetition[idx] = true;
            return 1;
        }else{
            if( SIGN(gyro[idx]) == SIGN(tmpReGyro[idx]) ){
                repetition[idx] = repetition[idx] + 1;
            }else{
                repetition[idx] = 1;
            }
            tmpReGyro[idx] = gyro[idx];
            return (1+C1)/(1+C1*(float)pow(repetition[idx],C2));
        }
    }

    // 좌우 흔들림으로 인한 미소한 오차를 줄임
    private float[] LowPassFilter(float[] gyro){
        if (!isInitLow) {
            lowGyro[0] = gyro[0];
            lowGyro[1] = gyro[1];
            lowGyro[2] = gyro[2];

            isInitLow = true;
        }

        lowGyro[0] = (T*gyro[0] + tau*lowGyro[0])/(T + tau);
        lowGyro[1] = (T*gyro[1] + tau*lowGyro[1])/(T + tau);
        lowGyro[2] = (T*gyro[2] + tau*lowGyro[2])/(T + tau);

        gyro[0] = lowGyro[0];
        gyro[1] = lowGyro[1];
        gyro[2] = lowGyro[2];

        return gyro;
    }

    // low-pass filter를 통과하며 지연된 신호를 보상
    private float[] Delagging(float[] gyro){
        if (!isInitDeLag) {
            delagGyro[0] = gyro[0];
            delagGyro[1] = gyro[1];
            delagGyro[2] = gyro[2];

            isInitDeLag = true;
        }

        tmpDelagGyro[0] = gyro[0] + tau/T*(gyro[0] - delagGyro[0]);
        tmpDelagGyro[1] = gyro[1] + tau/T*(gyro[1] - delagGyro[1]);
        tmpDelagGyro[2] = gyro[2] + tau/T*(gyro[2] - delagGyro[2]);

        delagGyro[0] = gyro[0];
        delagGyro[1] = gyro[1];
        delagGyro[2] = gyro[2];

        gyro[0] = tmpDelagGyro[0];
        gyro[1] = tmpDelagGyro[1];
        gyro[2] = tmpDelagGyro[2];

        return gyro;
    }
}
