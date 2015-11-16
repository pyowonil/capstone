package com.example.edge.myapplication;

/**
 * Created by edge on 2015-11-16.
 */
public class GyroToEulerAngle {
    private float m_anglex, m_angley, m_anglez;
    private float m_angle_deltax, m_angle_deltay, m_angle_deltaz;
    private float m_deltat;

    GyroToEulerAngle() {
        setOriginal(0, 0, 0);
        setTime(0.1f);
    }
    GyroToEulerAngle(float org_anglex, float org_angley, float org_anglez) {
        setOriginal(org_anglex, org_angley, org_anglez);
        setTime(0.1f);
    }
    GyroToEulerAngle(float org_anglex, float org_angley, float org_anglez, float delta_time) {
        setOriginal(org_anglex, org_angley, org_anglez);
        setTime(delta_time);
    }

    public void setOriginal(float org_anglex, float org_angley, float org_anglez) {
        m_anglex = org_anglex; m_angley = org_angley; m_anglez = org_anglez;
    }
    public void setTime(float delta_time) {
        m_deltat = delta_time;
    }
    public void setW(float wx, float wy, float wz) {
        float sinx = (float) Math.sin(m_anglex);
        float cosx = (float) Math.cos(m_anglex);
        float tany = (float) Math.tan(m_angley);
        float secy = (float) (1.0/Math.cos(m_angley));

        m_angle_deltax = wx + (wy*sinx*tany) + (wz*cosx*tany);
        m_angle_deltay = wy*cosx - wz*sinx;
        m_angle_deltaz = wy*sinx*secy + wz*cosx*secy;

        convert();
    }
    public void getAngle(float[] angles) {
        angles[0] = m_anglex;
        angles[1] = m_angley;
        angles[2] = m_anglez;
    }
    private void convert() {
        m_anglex += m_angle_deltax * m_deltat;
        m_angley += m_angle_deltay * m_deltat;
        m_anglez += m_angle_deltaz * m_deltat;
    }
}
