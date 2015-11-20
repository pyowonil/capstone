package com.example.edge.myapplication;

import Jama.Matrix;
/**
 * Created by pyowo on 2015-11-17.
 */
public class EulerKalman {
    // function[phi theta psi] <- EulerKalman(A, z)

    private Matrix H, Q, R, x, P;
    private Matrix A, z;
    private double wie = 15.0/(60*60), L=37.4481691, g=9.81;
    private Matrix C_bton, bton_tmp1, bton_tmp2, f, w, v;
    EulerKalman() {
        // H : 4 x 4 Matrix
        // Q : 4 x 4 Matrix
        // R : 4 x 4 Matrix
        // x : 4 x 1 Matrix
        // P : 4 x 4 Matrix
        H = new Matrix(new double[][]{
                {1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}
        });
        Q = new Matrix(new double[][]{
                {0.0001,0,0,0},{0,0.0001,0,0},{0,0,0.0001,0},{0,0,0,0.0001}
        });
        R = new Matrix(new double[][]{
                {10,0,0,0},{0,10,0,0},{0,0,10,0},{0,0,0,10}
        });
        x = new Matrix(new double[][]{
                {1},{0},{0},{0}
        });
        P = new Matrix(new double[][]{
                {1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}
        });

        // A : 4 x 4 Matrix
        // z : 4 x 1 Matrix
        A = new Matrix(new double[][]{
                {1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}
        });
        z = new Matrix(new double[][]{
                {0},{0},{0},{0}
        });

        // wie : 지구자전각속도
        // L : 위도
        // C_bton : 3 x 3 Matrix, Body frame to NED frame, N(North), E(East), D(Down?)
        C_bton = new Matrix(new double[][]{
                {0,0,0},{0,0,0},{0,0,0}
        });
        bton_tmp1 = new Matrix(new double[][]{
                {0,0,0},{0,0,0},{0,0,0}
        });
        bton_tmp2 = new Matrix(new double[][]{
                {0,0,0},{0,0,0},{0,0,0}
        });
        // f : 3 x 1 Matrix, accel datas
        // w : 3 x 1 Matrix, gyro datas
        // v : 3 x 1 Matrix, f x w
        f = new Matrix(new double[][]{
                {0},{0},{0}
        });
        w = new Matrix(new double[][]{
                {0},{0},{0}
        });
        v = new Matrix(new double[][]{
                {0},{0},{0}
        });
    }

    private Matrix xp, Pp, K;
    public double phi, theta, psi;
    public void run() {
        xp = A.times(x);
        Pp = A.times(P).times(A.transpose()).plus(Q);
        K = Pp.times(H.transpose()).times( (H.times(Pp).times(H.transpose()).plus(R).inverse()) );
        x = xp.plus( (K.times( (z.minus(H.times(xp))) )) );
        P = Pp.minus( (K.times(H).times(Pp)) );

        phi = Math.atan2(2*(x.get(2,0)*x.get(3,0)+x.get(0,0)*x.get(1,0)), 1-2*(x.get(1,0)*x.get(1,0)+x.get(2,0)*x.get(2,0)));
        theta = -Math.asin(2*(x.get(1,0)*x.get(3,0)-x.get(0,0)*x.get(2,0)));
        psi = Math.atan2(2*(x.get(1,0)*x.get(2,0)+x.get(0,0)*x.get(3,0)), 1-2*(x.get(2,0)*x.get(2,0)+x.get(3,0)*x.get(3,0)));
        v.set(0,0,phi);v.set(1,0,theta);v.set(2,0,psi);
        v = C_bton.times(v);
        phi = v.get(0,0); theta = v.get(1,0); psi = v.get(2,0);
    }

    public void set(double p, double q, double r, double dt, double fx, double fy, double fz) {
        A(p,q,r,dt);
        C_bton();
        z_accel(fx,fy,fz);
    }
    public void A(double p, double q, double r, double dt) {
        w.set(0,0,p);w.set(1,0,q);w.set(2,0,r);
        p = p * dt * 0.5;
        q = q * dt * 0.5;
        r = r * dt * 0.5;
        A.set(0,1,-p);  A.set(0,2,-q);  A.set(0,3,-r);
        A.set(1,0,p);                   A.set(1,2,r);   A.set(1,2,-q);
        A.set(2,0,q);   A.set(2,1,-r);                  A.set(2,3,p);
        A.set(3,0,r);   A.set(3,1,q);   A.set(3,2,-p);
    }
    public void z(double _phi, double _theta, double _psi) {
        _phi = _phi * 0.5;
        _theta = _theta * 0.5;
        _psi = _psi * 0.5;

        double sinhphi,coshphi,sinhtheta,coshtheta,sinhpsi,coshpsi;
        sinhphi = Math.sin(_phi);       coshphi = Math.cos(_phi);
        sinhtheta = Math.sin(_theta);   coshtheta = Math.cos(_theta);
        sinhpsi = Math.sin(_psi);       coshpsi = Math.cos(_psi);

        z.set(0,0, ( coshphi*coshtheta*coshpsi + sinhphi*sinhtheta*sinhpsi ));
        z.set(1,0, ( sinhphi*coshtheta*coshpsi - coshphi*sinhtheta*sinhpsi ));
        z.set(2,0, ( coshphi*sinhtheta*coshpsi + sinhphi*coshtheta*sinhpsi ));
        z.set(3,0, ( coshphi*coshtheta*sinhpsi - sinhphi*sinhtheta*coshpsi ));
    }

    public void z_accel(double fx, double fy, double fz) {
        double _phi = Math.atan(fy/fz);
        double _theta = Math.atan(fx/(Math.sqrt(fy*fy+fz*fz)));
        double sinphi,cosphi,sintheta,costheta;
        sinphi = Math.sin(_phi); cosphi = Math.cos(_phi); sintheta = Math.sin(_theta); costheta = Math.cos(_theta);
        double _psi = Math.atan( ((w.get(2,0)*sinphi-w.get(1,0)*cosphi)/(w.get(0,0)*costheta+w.get(1,0)*sinphi*sintheta+w.get(2,0)*cosphi*sintheta)) );
        z(_phi, _theta, _psi);
    }

    public void C_bton() {
        v.set(0, 0, f.get(1, 0)*w.get(2,0) - f.get(2, 0) * w.get(1, 0));
        v.set(1, 0, f.get(2,0)*w.get(0,0)-f.get(0,0)*w.get(2,0));
        v.set(2,0,f.get(0,0)*w.get(1, 0) - f.get(1,0)*w.get(0,0));
        double tanL = Math.tan(L);
        double cosL = Math.cos(L);

        bton_tmp1.set(0,0,-(tanL/g));
        bton_tmp1.set(2,0,-1/g);
        bton_tmp1.set(1,0,(1/(wie*cosL)));
        bton_tmp1.set(2,1,-(1/g*wie*cosL));

        bton_tmp2.set(0,0,f.get(0,0)); bton_tmp2.set(0,1,f.get(1,0)); bton_tmp2.set(0,2,f.get(2,0));
        bton_tmp2.set(1,0,w.get(0,0)); bton_tmp2.set(1,1,w.get(1,0)); bton_tmp2.set(1,2,w.get(2,0));
        bton_tmp2.set(2,0,v.get(0,0)); bton_tmp2.set(2,1,v.get(1,0)); bton_tmp2.set(2,2,v.get(2,0));

        C_bton = bton_tmp1.times(bton_tmp2);
    }

}
