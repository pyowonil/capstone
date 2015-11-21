package com.example.edge.myapplication;

import Jama.Matrix;

/**
 * Created by park on 2015-11-20.
 */
public class PosVelKF {

    private double PI = 3.14159265358979323846;
    private double DEG2RAD = (PI/180);
    private double RAD2DEG = (180/PI);
    private double gz = -9.81;
    double sigma_gps = 1.;

    // Velocity, Position에 대한 Kalman Filter의 상태벡터와 공분산
    private Matrix _X;	// vel_x, vel_y, vel_z, pos_x, pos_y, pos_z
    private Matrix _P;
    private Matrix _a;
    private Matrix A;		// For Debugging, 항법 좌표계에서의 가속도
    private Matrix g, f;
    Matrix I, F, G, Q;
    Matrix xd;
    Matrix Z, R, H, K;

    public PosVelKF(){
        _X = new Matrix(new double[][]{
                {0,0,0,0,0,0},
        }).transpose();

        _P = new Matrix(new double[][]{
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
        }).times(1.e10);

        g = new Matrix(new double[][]{
                {0,0,-1},
        }).transpose();

        f = new Matrix(new double[][]{
                {0,0,0},
        }).transpose();

        I = new Matrix(new double[][]{
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
        });

        F = new Matrix(new double[][]{
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
        });

        G = new Matrix(new double[][]{
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
        });

        Q = new Matrix(new double[][]{
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
        });

        xd = new Matrix(new double[][]{
                {0,0,0,0,0,0},
        }).transpose();

        Z = new Matrix(new double[][]{
                {0,0,0},
        }).transpose();

        R = new Matrix(new double[][]{
                {0,0,0},
                {0,0,0},
                {0,0,0},
        });

        H = new Matrix(new double[][]{
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
        });
    }

    public void predict(Matrix Rn, float[] gyro, float[] acc, double dt){
        // Velocity, Position Predict 과정
        f.set(0,0,acc[0]); f.set(1,0,acc[1]); f.set(2,0,acc[2]);

        Matrix vd = Rn.times(f).minus(g).times(gz);
        xd.set(0,0,vd.get(0,0)); xd.set(1,0,vd.get(1,0)); xd.set(2,0,vd.get(2,0));
        xd.set(3,0,_X.get(0,0)); xd.set(4,0,_X.get(1,0)); xd.set(5,0,_X.get(2,0));

        F.set(3,0,1); F.set(4,1, 1); F.set(5,2,1);

        G.set(0,0,Rn.get(0,0)); G.set(0,1,Rn.get(0,1)); G.set(0,2,Rn.get(0,2));
        G.set(1,0,Rn.get(1,0)); G.set(1,1,Rn.get(1,1)); G.set(1,2,Rn.get(1,2));
        G.set(2,0,Rn.get(2,0)); G.set(2,1,Rn.get(2,1)); G.set(2,2,Rn.get(2,2));
        G.set(0,3,-1); G.set(1,4,-1); G.set(2,5,-1);

        double sw = 5*DEG2RAD*Math.sqrt(gyro[0] * gyro[0] + gyro[1] * gyro[1] + gyro[2] * gyro[2]);
        double sa = 5*gz*Math.abs(Math.sqrt(acc[0] * acc[0] + acc[1] * acc[1] + acc[2] * acc[2]) - 1.);

        double tmp = 0.01 + sw*sw + sa*sa;
        double tmp2 = 0.1*0.1;
        Q.set(0,0,tmp); Q.set(1,1,tmp); Q.set(2,2,tmp);
        Q.set(3,3,tmp2); Q.set(4,4,tmp2); Q.set(5,5,tmp2);

        A = I.plus((F.times(dt)));
        _X = _X.plus(xd.times(dt));
        _P = A.times(_P.times(A.transpose())).plus(G.times(Q.times(G.transpose().times(dt*dt))));

        _a = vd;	// For Debugging
    }

    public Matrix get_a(){
        return _a;
    }

    public Matrix get_X(){
        return _X;
    }

    public Matrix get_P(){
        return _P;
    }

    public Matrix getQ(){
        return Q;
    }

    public void update(Matrix Rn, double pos_x, double pos_y, double pos_z){

        Z.set(0,0,pos_x); Z.set(1,0,pos_y); Z.set(2,0,pos_z);

        R.set(0,0,sigma_gps*sigma_gps);
        R.set(1,1,sigma_gps*sigma_gps);
        R.set(2, 2, sigma_gps * sigma_gps);

        H.set(0,3,1);
        H.set(1,4,1);
        H.set(2,5,1);
        K = H.times(_P.times(H.transpose())).plus(R);
        K = _P.times(H.transpose()).times( K.inverse());

        _P = (I.minus((K.times(H)))).times(_P);
        _X = _X.plus((K.times((Z.minus((H.times(_X)))))));
    }

    double vel_x = 0;
    double sigma_x = 1.e8;

    public void update(Matrix Rn){
        // 차량은 전후진 방향으로만 운동이 가능하고 차량의 좌우, 상하로 속도가 0이다.
        // 이러한 제약 조건을 이용하여 차량의 속도를 수정한다.

        Matrix Z = new Matrix(new double[][]{
                {vel_x, 0, 0},
        }).transpose();

        Matrix R = new Matrix(new double[][]{
            {sigma_x, 0,0},
            {0, 0.01*0.01, 0},
            {0, 0, 0.01*0.01},
        });

        Matrix RnI = Rn.transpose();

        Matrix I = new Matrix(new double[][]{
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
        });

        Matrix H = new Matrix(new double[][]{
                {Rn.get(0,0),Rn.get(0,1),Rn.get(0,2),0,0,0},
                {Rn.get(1,0),Rn.get(1,1),Rn.get(1,2),0,0,0},
                {Rn.get(2,0),Rn.get(2,1),Rn.get(2,2),0,0,0},
        });

        Matrix K = _P.times(H.transpose()).times(((H.times(_P).times(H.transpose())).plus(R)).inverse());

        _P = I.minus((K.times(H))).times(_P);
        _X = _X.plus((K.times(Z.minus(H.times(_X)))));
    }

    public void update(Matrix Rn, double vel_x, double sigma_x){
        // 차량은 전후진 방향으로만 운동이 가능하고 차량의 좌우, 상하로 속도가 0이다.
        // 이러한 제약 조건을 이용하여 차량의 속도를 수정한다.

        Matrix Z = new Matrix(new double[][]{
                {vel_x, 0, 0},
        }).transpose();

        Matrix R = new Matrix(new double[][]{
                {sigma_x, 0,0},
                {0, 0.01*0.01, 0},
                {0, 0, 0.01*0.01},
        });

        Matrix RnI = Rn.transpose();

        Matrix I = new Matrix(new double[][]{
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
        });

        Matrix H = new Matrix(new double[][]{
                {Rn.get(0,0),Rn.get(0,1),Rn.get(0,2),0,0,0},
                {Rn.get(1,0),Rn.get(1,1),Rn.get(1,2),0,0,0},
                {Rn.get(2,0),Rn.get(2,1),Rn.get(2,2),0,0,0},
        });

        Matrix K = _P.times(H.transpose()).times(((H.times(_P).times(H.transpose())).plus(R)).inverse());

        _P = I.minus((K.times(H))).times(_P);
        _X = _X.plus((K.times(Z.minus(H.times(_X)))));
    }
}
