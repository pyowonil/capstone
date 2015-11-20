package com.example.edge.myapplication;

import Jama.Matrix;
/**
 * Created by pyowo on 2015-11-17.
 */
public class EulerKalman {
    // ---------- * ---------- * ---------- * ---------- Variables ---------- * ---------- * ---------- * ----------
    // 잡음의 공분산 행렬 Q, R (4 x 4)
    private Matrix m_Q;
    private Matrix m_R;
    // 상태변수 (쿼터니언) x (4 x 1)
    private Matrix m_x;
    private Matrix m_xp;
    // 오차 공분산 행렬 P (4 x 4)
    private Matrix m_P;
    private Matrix m_Pp;
    // 상태전이행렬 A (4 x 4)
    private Matrix m_A;
    // 측정값 z (4 x 1)
    private Matrix m_z;
    // 칼만이득 K (4 x 4)
    private Matrix m_K;

    //private Matrix m_I;

    // Euler Angles
    private EulerAngles m_euler_angles = new EulerAngles();

    // 결과값
    private double phi=0, theta=0, psi=0;

    private long starttime = System.currentTimeMillis();

    // ---------- * ---------- * ---------- * ---------- Methods ---------- * ---------- * ---------- * ----------
    // 생성자
    EulerKalman() {
        m_Q = new Matrix(new double[][]{ {0.0001,0,0,0}, {0,0.0001,0,0}, {0,0,0.0001,0}, {0,0,0,0.0001} });
        m_R = new Matrix(new double[][]{ {10,0,0,0}, {0,10,0,0}, {0,0,10,0}, {0,0,0,10} });
        m_x = new Matrix(new double[][]{ {1},{0},{0},{0} });
        m_xp = new Matrix(new double[][]{ {1},{0},{0},{0} });
        m_P = new Matrix(new double[][]{ {1,0,0,0}, {0,1,0,0}, {0,0,1,0}, {0,0,0,1} });
        m_Pp = new Matrix(new double[][]{ {1,0,0,0}, {0,1,0,0}, {0,0,1,0}, {0,0,0,1} });
        m_A = new Matrix(new double[][]{ {1,0,0,0}, {0,1,0,0}, {0,0,1,0}, {0,0,0,1} });
        m_z = new Matrix(new double[][]{ {1},{0},{0},{0} });
        m_K = new Matrix(new double[][]{ {1,0,0,0}, {0,1,0,0}, {0,0,1,0}, {0,0,0,1} });
        //m_I = new Matrix(new double[][]{ {1,0,0,0}, {0,1,0,0}, {0,0,1,0}, {0,0,0,1} });
    }

    // 1. set A
    public void setA(double p, double q, double r, long time) {
        if(starttime == -1) starttime = time;
        double dt = (time - starttime)*0.0001; // 단위 sec
        starttime = time;
        double hp = time*p*0.5;
        double hq = time*q*0.5;
        double hr = time*r*0.5;
        m_A.set(0,0,1);         m_A.set(0,1,-hp);       m_A.set(0,2,-hq);       m_A.set(0,3,-hr);
        m_A.set(1,0,hp);        m_A.set(1,1,1);         m_A.set(1,2,hr);        m_A.set(1,3,-hq);
        m_A.set(2,0,hq);        m_A.set(2,1,-hr);       m_A.set(2,2,1);        m_A.set(2,3,hp);
        m_A.set(3,0,hr);        m_A.set(3,1,hq);        m_A.set(3,2,-hp);       m_A.set(3,3,1);
    }

    // 2 - 1. set phi, theta
    public void setAccel(double fx, double fy, double fz) {
        m_euler_angles.setF(fx, fy, fz);
    }
    // 2 - 2. set psi
    public void setMagnetic(double hx, double hy, double hz) {
        m_euler_angles.setH(hx, hy, hz);
    }

    // 3. set z
    public void setz() {
        double sinphi = Math.sin(m_euler_angles.getPhi());
        double cosphi = Math.cos(m_euler_angles.getPhi());
        double sintheta = Math.sin(m_euler_angles.getTheta());
        double costheta = Math.cos(m_euler_angles.getTheta());
        double sinpsi = Math.sin(m_euler_angles.getPsi());
        double cospsi = Math.cos(m_euler_angles.getPsi());
        m_z.set(0,0,cosphi*costheta*cospsi + sinphi*sintheta*sinpsi);
        m_z.set(1,0,sinphi*costheta*cospsi - cosphi*sintheta*sinpsi);
        m_z.set(2,0,cosphi*sintheta*cospsi + sinphi*costheta*sinpsi);
        m_z.set(3,0,cosphi*costheta*sinpsi - sinphi*sintheta*cospsi);
    }

    // 4. get phi, theta, psi
    public void estimate() {
        m_xp = m_A.times(m_x);
        m_Pp = m_A.times(m_P).times(m_A.transpose()).plus(m_Q);
        m_K = m_Pp.times((m_Pp.plus(m_R).inverse()));
        m_x = m_xp.plus(m_K.times(m_z.minus(m_xp)));
        m_P = m_Pp.minus(m_K.times(m_Pp));

        double a=m_x.get(0,0), b=m_x.get(1,0), c=m_x.get(2,0), d=m_x.get(3,0);
        phi = Math.atan2(2*(c*d+a*b), 1-2*(b*b+c*c));
        theta = -Math.asin(2*(b*d-a*c));
        psi = Math.atan2(2*(b*c+a*d), 1-2*(c*c+d*d));
    }

    // get 함수
    public double getPhi() {
        return phi;
    }
    public double getTheta() {
        return theta;
    }
    public double getPsi() {
        return psi;
    }
}
