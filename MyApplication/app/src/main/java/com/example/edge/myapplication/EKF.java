package com.example.edge.myapplication;

import Jama.Matrix;

public class EKF {
    // Euler 방법으로 수치해석적 적분하는 과정은 다음과 같은 이산 식으로 표현
    // x(k+1) = x(k) + iC * w(k) * dt
    // 자이로 센서에서 측정한 각속도 [x, y, z]
    private Matrix m_w;
    // w 를 오일러 각(상태변수) [ax, ay, az]
    // ax: Euler angle of x, ay: Euler angle of y, az: Euler angle of z
    private Matrix m_x;
    // inverse C
    private Matrix m_iC;
    // delta time
    private double m_dt;

    private Matrix m_I;
    private Matrix m_P;
    // 상태전이행렬로 시스템 모델의 각 변수에 대한 자코비안이다.
    private Matrix m_A;
    private Matrix m_B;
    // 시스템 모델의 입력에 대한 공분산 행렬로 임의로 다음과 같이 설정하였다.
    private Matrix m_Q;

    // 측정 모델
    private Matrix m_H;
    // Kalman gain
    private Matrix m_K;
    private Matrix m_a;
    private Matrix m_R1;
    private Matrix m_R8;

    private Matrix m_z;
    private Matrix m_g;

    EKF() {
        m_w = new Matrix(new double[][]{
                {0},
                {0},
                {0}
        });
        m_x = new Matrix(new double[][]{
                {0},
                {0},
                {0}
        });
        m_iC = new Matrix(new double[][]{
                {1, 0, 0},
                {0, 0, 0},
                {0, 0, 0}
        });
        m_I = new Matrix(new double[][]{
                {1, 0, 0},
                {0, 1, 0},
                {0, 0, 1}
        });
        m_P = new Matrix(new double[][]{
                {1, 0, 0},
                {0, 1, 0},
                {0, 0, 1}
        });
        m_A = new Matrix(new double[][]{
                {0, 0, 0},
                {0, 0, 0},
                {0, 0, 0}
        });
        m_B = new Matrix(new double[][]{
                {0, 0, 0},
                {0, 0, 0},
                {0, 0, 0}
        });
        m_Q = new Matrix(new double[][]{
                {1, 0, 0},
                {0, 1, 0},
                {0, 0, 1}
        });
        m_dt = 0.0;

        m_H = new Matrix(new double[][]{
                {1, 0, 0},
                {0, 1, 0}
        });
        m_a = new Matrix(new double[][]{
                {0, 0},
                {0, 0}
        });
        m_K = new Matrix(new double[][]{
                {0, 0},
                {0, 0},
                {0, 0}
        });
        m_R1 = new Matrix(new double[][]{
                {1, 0},
                {0, 1}
        });
        m_R8 = new Matrix(new double[][]{
                {5012*5012, 0},
                {0, 5012*5012}
        });
        m_z = new Matrix(new double[][]{
                {0},
                {0}
        });
        m_g = new Matrix(new double[][]{
                {0},
                {0},
                {-9.81}
        });
    }

    public void predict() {
        Predict();
    }
    public void correct(double[] xyz) {
        set_w(xyz);
        set_z();
        Correct();
    }
    public void correct(double x, double y, double z) {
        set_w(x, y, z);
        set_z();
        Correct();
    }
    public Matrix mat() {
        return m_x;
    }
    public double x() {
        return m_x.get(0, 0);
    }
    public double y() {
        return m_x.get(1, 0);
    }
    public double z() {
        return m_x.get(2, 0);
    }

    public void set_dt(double dt) {
        //System.out.println("[+] EKF: run set_dt");
        m_dt = dt;
    }

    private void set_iC() {
        //System.out.println("[+] EKF: run set_iC");
        // {1	sin(ax)tan(ay)	cos(ax)tan(ay)	}
        // {0	cos(ax)			-sin(ax)		}
        // {0	sin(ax)/cos(ay)	cos(ax)/cos(ay)	}
        double ax = m_x.get(0, 0);
        double ay = m_x.get(1, 0);
        double sinax = Math.sin(ax);
        double cosax = Math.cos(ax);
        double tanay = Math.tan(ay);
        double cosay = Math.cos(ay);

        m_iC.set(0, 1, sinax*tanay);	m_iC.set(0, 2, cosax*tanay);
        m_iC.set(1, 1, cosax);			m_iC.set(1, 2, -sinax);
        m_iC.set(2, 1, sinax/cosay);	m_iC.set(2, 2, cosax/cosay);;
    }

    private void set_A() {
        //System.out.println("[+] EKF: run set_A");
        double wx = m_w.get(0, 0);
        double wy = m_w.get(1, 0);
        double wz = m_w.get(2, 0);
        double ax = m_x.get(0, 0);
        double ay = m_x.get(1, 0);
        double az = m_x.get(2, 0);
        double sinax = Math.sin(ax);
        double cosax = Math.cos(ax);
        double sinay = Math.sin(ay);
        double cosay = Math.cos(ay);
        double cos2ay = cosay*cosay;

        m_A.set(0, 0, sinay*(wy*cosax-wz*sinax)/cosay +1);
        m_A.set(0, 1, (wy*sinax+wz*cosax)/cos2ay);
        m_A.set(0, 2, 0);

        m_A.set(1, 0, -wy*sinax-wz*cosax);
        m_A.set(1, 1, 0 +1);
        m_A.set(1, 2, 0);

        m_A.set(2, 0, (wy*cosax-wz*sinax)/cosay);
        m_A.set(2, 1, sinay*(wy*sinax+wz*cosax)/cos2ay);
        m_A.set(2, 2, 0 +1);
    }

    private void set_B() {
        //System.out.println("[+] EKF: run set_B");
        m_B = m_iC.times(m_dt);
    }

    private void Predict() {
        //System.out.println("[+] EKF: run Predict");
        set_iC(); set_A(); set_B();
        // (1) Project the state ahead
        // x(^-k) = A * x(^k-1) + B * u(k-1)
        m_x = m_x.plus(m_iC.times(m_w).times(m_dt));

        // (2) Project the error covariance ahead
        // P(-k) = A*P(k-1)*A(T) + Q
        m_P = m_A.times(m_P).times(m_A.transpose()).plus(m_B.times(m_Q.times(m_B.transpose())));
        //System.out.println("[+] EKF: end Predict");
    }

    private void Correct() {
        //System.out.println("[+] EKF: run Correct");
        m_a = m_g;
        m_a.set(0, 0, m_a.get(0, 0)*-Math.sin(m_x.get(1, 0))+m_w.get(1,0)-m_w.get(2, 0) );
        m_a.set(1, 0, m_a.get(1, 0)*Math.cos(m_x.get(1, 0))*Math.sin(m_x.get(0, 0))-m_w.get(0, 0)+m_w.get(2, 0));
        m_a.set(2, 0, m_a.get(2, 0)*Math.cos(m_x.get(1, 0))*Math.cos(m_x.get(0, 0))+m_w.get(0, 0)-m_w.get(1, 0));
        Matrix R;
        double norm = m_a.norm1();
        if(9.3 <= norm && norm <= 10.3 ) { // 9.8 +- 0.5
            R = m_R1;
        } else {
            R = m_R8;
        }

        // (1) Compute the Kalman gain
        // K(k) = P(-k)*H(T)*(H*P(-k)*H(T)+R).inverse
        m_K = m_P.times(m_H.transpose().times( (m_H.times(m_P).times(m_H.transpose()).plus(R)).inverse() ));

        // (2) Update estimate with measurement z(k)
        // x(^k) = x(^-k) + K(k) * (z(k) - H*x(^-k))
        m_x = m_x.plus(m_K.times((m_z.minus(m_H.times(m_x)))));

        // (3) Update the error covariance
        // P(k) = P(-k) - K(k)*H(k)*P(-k)
        m_P = m_P.minus((m_K.times(m_H).times(m_P)));
        //System.out.println("[+] EKF: end Correct");
    }

    private void set_z() {
        //System.out.println("[+] EKF: run set_z");
        m_z.set(0, 0, m_w.get(0, 0));
        m_z.set(1, 0, m_w.get(1, 0));
    }

    private void set_w(double[] xyz) {
        m_w.set(0, 0, xyz[0]);
        m_w.set(1, 0, xyz[1]);
        m_w.set(2, 0, xyz[2]);
    }
    private void set_w(double x, double y, double z) {
        m_w.set(0, 0, x);
        m_w.set(1, 0, y);
        m_w.set(2, 0, z);
    }
}
