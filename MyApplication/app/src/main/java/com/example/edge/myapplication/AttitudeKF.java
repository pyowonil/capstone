package com.example.edge.myapplication;

import Jama.Matrix;

/**
 * Created by park on 2015-11-20.
 */
public class AttitudeKF {

    private double PI = 3.14159265358979323846;
    private double DEG2RAD = (PI/180);
    private double RAD2DEG = (180/PI);
    private double eps = 1.e-6;
    private double sigma_gps = 30.;
    private double epsilon = 1.e-8;

    // Attitude에 대한 KF의 상태벡터와 분산
    private Matrix _X; // 3x3 회전행렬 R
    private Matrix _P;
    private Matrix R, K, M;
    private Matrix C0, C1;

    private double roll, pitch, yaw;
    private double[] sigma;


    public AttitudeKF(){
        _X = new Matrix(new double[][]{
                {0, 0, 0},
                {0, 0, 0},
                {0, 0, 0},
        });
        _P = new Matrix(new double[][]{
                {0, 0, 0},
                {0, 0, 0},
                {0, 0, 0},
        }).times(1.e10);

        R = new Matrix(new double[][]{
                {0, 0, 0},
                {0, 0, 0},
                {0, 0, 0},
        });

        M = new Matrix(new double[][]{
                {0, 0, 0},
                {0, 0, 0},
                {0, 0, 0},
        });

        sigma = new double[3];
    }

    public Matrix get_X(){
        return _X;
    }

    public void predict(float[] gyro, double dt){
        // Attitude Predict 과정
        sigma[0] = 10. + DEG2RAD*gyro[0];
        sigma[1] = 10. + DEG2RAD*gyro[1];
        sigma[2] = 10. + DEG2RAD*gyro[2];

        GetRPY(roll, pitch, yaw);
        C1 = C1I(roll, pitch, yaw);
        RotationMatrix(DEG2RAD*gyro[0]*dt, DEG2RAD*gyro[1]*dt, DEG2RAD*gyro[2]*dt);
        _X = _X.times(R);
        Diag(sigma[0],sigma[1],sigma[2]);
        _P = _P.plus((C1.times(M).times((C1.transpose())).times(dt*dt)));

        // 회전행렬을 orthogonal matrix로 만들기
        //_R = 2.*_R*!(!_R*_R + ~_R*_R);
    }

    public void update(float[] gyro, float[] acc){
        // Attitude Update by Gravity 과정

        // 센서가 움직이지 않는 상황에서 항상 중력 가속도는 아래 방향(-z)으로 작용하고 있기
        // 때문에, 센서가 움직이지 않을 때는 가속도 벡터(g)는 중력과 동일하다. 그래서
        // 가속도 벡터의 자세로부터 현재 자세 _x를 보정할 수 있다.

        // accl과 magn은 센서 좌표계를 기준으로 측정된 값이다.
        // 센서의 자세(R)을 곱해서 전역좌표계를 기준으로 한 값으로 바꿔준다.
//        dMatrix g = _x*dMatrix (3,1, accl);
//
//        // 중력으로 찾은 각도의 오차를 업데이트 하는 비율(이득)을 정한다.
//        // 중력 벡터의 크기가 1.근처일 때 이득이 커야하고 1.에서 멀어질 수록 이득이 적어야 한다.
//        double sw = _DEG2RAD*sqrt(gyro[0]*gyro[0] + gyro[1]*gyro[1] + gyro[1]*gyro[1]);
//        double sa = fabs(sqrt (accl[0]*accl[0] + accl[1]*accl[1] + accl[2]*accl[2]) - 1.);
//        double sigma = 0.1 + 100*sa + 100*sw;
//
//        // 각도의 보정량을 계산한다.
//        double delta_phi   = atan2 (-g(1,0), -g(2,0));
//        double delta_theta = (-1 < g(0,0) && g(0,0) < 1) ? asin(-g(0,0)/-1.) : 0.;
//
//        double roll, pitch, yaw;
//        GetRPY (&roll, &pitch, &yaw);
//        dMatrix C0 = C0I(roll, pitch, yaw);
//
//        dMatrix K = _P*!(_P + C0*Diag(sigma, sigma, 1.e8)*~C0);
//
//        _x = RotationMatrix (K(0,0)*delta_phi, K(1,1)*delta_theta, 0)*_x;
//        _P = _P - K*_P;
    }

    public void update(double dir, double vel){
        // GPS에 의한 Attitude Update 과정
        vel = (1 < vel) ? vel - 1 : 0.f;	// 속도가 1 이상일 때만 _x 업데이트

        double psi = Math.atan2(_X.get(1, 0), _X.get(0,0));
        double sigma_psi = 2*sigma_gps/(epsilon + vel);

        GetRPY (roll, pitch, yaw);
        C0 = C0I(roll, pitch, yaw);
        Diag(1.e10, 1.e10, sigma_psi);
        K = _P.times((_P.plus(C0.times(M).times(C0.transpose()))).inverse());
        RotationMatrix(0, 0, K.get(2,2)*DeltaRad (dir, psi));
        _X = R.times(_X);
        _P = _P.minus((K.times(_P)));
    }

    private Matrix C0I(double phi, double theta, double psi){
        double sp = Math.sin(psi);
        double cp = Math.cos(psi);
        double ct = Math.cos(theta);
        double tt = Math.tan(theta);

        if (-eps < ct && ct < 0) ct = -eps;
        if (0 <= ct && ct < eps) ct = +eps;

        Matrix C_ = new Matrix(new double[][]{
                { cp/ct,	sp/ct,	0	},
                { -sp,		cp,		0	},
                { cp*tt,	sp*tt,	1	},
        });

        return C_;
    }

    private Matrix C1I(double phi, double theta, double psi){
        double sp = Math.sin(phi);
        double cp = Math.cos(phi);
        double ct = Math.cos(theta);
        double tt = Math.tan(theta);

        if (-eps < ct && ct < 0) ct = -eps;
        if (0 <= ct && ct < eps) ct = +eps;

        Matrix C_ = new Matrix(new double[][]{
            { 1,	sp*tt,		cp*tt	},
            { 0,	cp,			-sp		},
            { 0,	sp/ct,		cp/ct	},
        });

        return C_;
    }

    public void GetRPY(double roll, double pitch, double yaw){
        roll = Math.atan2(_X.get(2,1), _X.get(2,2));
        pitch = Math.atan2(-_X.get(2,0), Math.sqrt(_X.get(2,1)*_X.get(2,1)+_X.get(2,2)*_X.get(2,2)));
        yaw = Math.atan2(_X.get(1,0), _X.get(0,0));
    }

    private void RotationMatrix(double phi, double theta, double psi){
        double sin_phi = Math.sin(phi),		cos_phi = Math.cos(phi);
        double sin_tht = Math.sin(theta),	cos_tht = Math.cos(theta);
        double sin_psi = Math.sin(psi),		cos_psi = Math.cos(psi);

/*
	// 변환 순서: Rx(phi) x Ry(theta) x Rz(psi)
	A(0,0) =  cos_tht*cos_psi;								A(0,1) = -cos_tht*sin_psi;								A(0,2) =  sin_tht;
	A(1,0) =  sin_phi*sin_tht*cos_psi + cos_phi*sin_psi;	A(1,1) = -sin_phi*sin_tht*sin_psi + cos_phi*cos_psi;	A(1,2) = -sin_phi*cos_tht;
	A(2,0) = -cos_phi*sin_tht*cos_psi + sin_phi*sin_psi;	A(2,1) =  cos_phi*sin_tht*sin_psi + sin_phi*cos_psi;	A(2,2) =  cos_phi*cos_tht;
*/
        // 변환 순서: Rz(psi) x Ry(theta) x Rx(phi)
        R.set(0,0,cos_psi*cos_tht);     R.set(0,1,cos_psi*sin_phi*sin_tht - cos_phi*sin_psi);   R.set(0,2,sin_phi*sin_psi + cos_phi*cos_psi*sin_tht);
        R.set(1,0,cos_tht*sin_psi);     R.set(1,1,cos_phi*cos_psi + sin_phi*sin_psi*sin_tht);   R.set(1,2,cos_phi*sin_psi*sin_tht - cos_psi*sin_phi);
        R.set(2,0,-sin_tht);            R.set(2,1,cos_tht*sin_phi);                             R.set(2,2,cos_phi*cos_tht);
    }

    private void Diag(double d0, double d1, double d2){
        M.set(0,0,d0);
        M.set(1,1,d1);
        M.set(2,2,d2);
    }

    private double DeltaRad(double ang1, double ang2){
        double da = ang1 - ang2;
        if (-PI < da && da < PI) return da;
        else {
            da = da%(2*PI);
            if (PI <= da) return da - 2*PI;
            else if (da <= -PI) return da + 2*PI;
            else return da;
        }
    }

}
