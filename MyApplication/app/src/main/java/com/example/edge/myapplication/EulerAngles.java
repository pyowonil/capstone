package com.example.edge.myapplication;

import Jama.Matrix;
/**
 * Created by pyo on 2015-11-21.
 */
public class EulerAngles {
    // ---------- * ---------- * ---------- * ---------- Variables ---------- * ---------- * ---------- * ----------
    // ---------- Constants ----------
    // 중력 가속도
    //public final double m_g = 9.8;
    // 지구자전각속도
    //public final double m_W = 0.000073;

    // ---------- Setting Variables ----------
    // 위도, 37.4507388은(는) 하이테크 위도
    //private double m_L = 37.4507388;

    // ---------- Getting Variables ----------
    private double phi=0, theta=0, psi=0;

    // ---------- Navigation frame (지구 표면 좌표계) ----------
    // 가속도(x, y, z)
    //private Matrix m_n_f;
    // 각속도(x, y, z)
    private Matrix m_n_w;

    // ---------- Body frame (동체 좌표계) ----------
    // 가속도(x, y, z)
    private Matrix m_b_f;
    // 각속도(x, y, z)
    private Matrix m_b_w;

    // ---------- Convert Matrix ----------
    // Convert Navigation frame to Body frame Matrix
    //private Matrix m_nCb;

    // Convert Body frame to Navigation frame Matrix
    private Matrix m_bCn;

    // Convert m_b_w to m_n_w Matrix, useing M * m_b_w -> m_n_w
    private Matrix m_C1T;

    // ---------- * ---------- * ---------- * ---------- Methods ---------- * ---------- * ---------- * ----------
    // ---------- 생성자 ----------
    EulerAngles() {
        // Matrix initialization
        // 지구 표면 좌표계의 가속도 (고정)
        //m_n_f = new Matrix(new double[][]{
        //        {0},{0},{-m_g}
        //});
        // 지구 표면 좌표계의 각속도 (반고정)
        m_n_w = new Matrix(new double[][]{
                {0},{0},{0}
        });
        // 동체 좌표계의 가속도
        m_b_f = new Matrix(new double[][]{
                {0},{0},{0}
        });
        // 동체 좌표계의 각속도
        m_b_w = new Matrix(new double[][]{
                {0},{0},{0}
        });
        // 변환 행렬 C1의 transpose 형태
        m_C1T = new Matrix(new double[][]{
                {0,0,0},{0,0,0},{0,0,0}
        });
        m_bCn = new Matrix(new double[][]{
                {0,0,0},{0,0,0},{0,0,0}
        });
    }
    // ---------- set 함수 ----------
    // 위도 설정
    //public void setL(double L) {
    //    m_L = L;
    //}
    // Body frame 의 가속도 설정
    public void setF(double x, double y, double z) {
        m_b_f.set(0,0,x);
        m_b_f.set(1,0,y);
        m_b_f.set(2,0,z);
        phi = Math.atan(y/z);
        theta = Math.atan(x/Math.sqrt(y*y+z*z));
        setC1T();
    }
    // Body frame 의 각속도 설정
    //public void setW(double x, double y, double z) {
    //    m_b_w.set(0,0,x);
    //    m_b_w.set(1,0,y);
    //    m_b_w.set(2,0,z);
    //    m_n_w = m_C1T.times(m_b_w);
    //    psi = -Math.atan(m_n_w.get(1,0)/m_n_w.get(0,0));
    //}
    // Body frame 의 각속도 대신 자기장 센서 설정
    public void setH(double x, double y, double z) {
        m_b_w.set(0,0,x);
        m_b_w.set(1,0,y);
        m_b_w.set(2,0,z);
        m_n_w = m_C1T.times(m_b_w);
        psi = Math.atan(m_n_w.get(1, 0) / m_n_w.get(0, 0));
        setbCn();
    }
    // C1T 생성
    private void setC1T() {
        double sinphi = Math.sin(phi);
        double cosphi = Math.cos(phi);
        double sintheta = Math.sin(theta);
        double costheta = Math.cos(theta);

        m_C1T.set(0,0,costheta);    m_C1T.set(0,1,sinphi*sintheta); m_C1T.set(0,2,cosphi*sintheta);
        m_C1T.set(1,0,0);           m_C1T.set(1,1,cosphi);          m_C1T.set(1,2,-sinphi);
        m_C1T.set(2,0,-sintheta);   m_C1T.set(2,1,sinphi*costheta); m_C1T.set(2,2,cosphi*costheta);
    }
    private void setbCn() {
        double sinphi = Math.sin(phi);
        double cosphi = Math.cos(phi);
        double sintheta = Math.sin(theta);
        double costheta = Math.cos(theta);
        double sinpsi = Math.sin(psi);
        double cospsi = Math.cos(psi);

        //m_bCn.set(0,0,costheta*cospsi);                         m_bCn.set(0,1,costheta*sinpsi);                         m_bCn.set(0,2,-sintheta);
        //m_bCn.set(1,0,-cosphi*sinpsi+sinphi*sintheta*cospsi);   m_bCn.set(1,1,cosphi*cospsi+sinphi*sintheta*sinpsi);    m_bCn.set(1,2,sinphi*costheta);
        //m_bCn.set(2,0,sinphi*sinpsi+cosphi*sintheta*cospsi);    m_bCn.set(2,1,-sinphi*cospsi+cosphi*sintheta*sinpsi);   m_bCn.set(2,2,cosphi*costheta);
        m_bCn.set(0,0,costheta*cospsi);     m_bCn.set(0,1,-cosphi*sinpsi+sinphi*sintheta*cospsi);       m_bCn.set(0,2,sinphi*sinpsi+cosphi*sintheta*cospsi);
        m_bCn.set(1,0,costheta*sinpsi);     m_bCn.set(1,1,cosphi*cospsi+sinphi*sintheta*sinpsi);        m_bCn.set(1,2,-sinphi*cospsi+cosphi*sintheta*sinpsi);
        m_bCn.set(2,0,-sintheta);           m_bCn.set(2,1,sinphi*costheta);                             m_bCn.set(2,2,cosphi*costheta);
    }

    // ---------- get 함수 ----------
    public double getPhi() {
        return phi;
    }
    public double getTheta() {
        return theta;
    }
    public double getPsi() {
        return psi;
    }
    public Matrix getbCn() {
        return m_bCn;
    }
    public float[] getFn(float[] Fb){
        m_b_f.set(0,0,Fb[0]); m_b_f.set(1,0,Fb[1]); m_b_f.set(2,0,Fb[2]);
        m_b_f = m_bCn.times(m_b_f);
        Fb[0] = (float) m_b_f.get(0,0); Fb[1] = (float) m_b_f.get(1,0); Fb[2] = (float) m_b_f.get(2,0);
        return Fb;
    }
}
