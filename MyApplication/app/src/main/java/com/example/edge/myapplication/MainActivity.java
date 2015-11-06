package com.example.edge.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // text view
    private TextView m_txtv_simpson;
    private TextView m_txtv_wifi;
    private TextView m_txtv_gps;

    // button
    private Button m_btn_gpsinfo;

    // gps object
    private GpsInfo m_gps;

    // (#calculate) simpson object
    private Simpson m_simpson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set gps object
        m_gps = new GpsInfo(MainActivity.this);

        // set simpson
        m_simpson = new Simpson();

        m_txtv_simpson = (TextView)findViewById(R.id.simpson);
        m_txtv_wifi = (TextView)findViewById(R.id.wifiscan);
        m_txtv_gps = (TextView)findViewById(R.id.gpsinfo);

        m_btn_gpsinfo = (Button)findViewById(R.id.btn_gpsinfo);

        m_btn_gpsinfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg) {
                // gps 사용 유무 가져오기
                if(m_gps.isGetLocation()) {
                    double latitude, longitude;
                    latitude = m_gps.getLatitude();
                    longitude = m_gps.getLongitude();
                    String gpsinfo = "Latitude   : " + Double.toString(latitude) + "\nLongitude : " + Double.toString(longitude);
                    m_txtv_gps.setText(gpsinfo);
                } else {
                    m_gps.showSettingsAlert();
                    String gpsinfo = "Latitude   : " + "Error" + "\nLongitude : " + "Error";
                    m_txtv_gps.setText(gpsinfo);
                }
            }
        });

        String flowInfo = "";
        int n = 6;
        float x[] = new float[n+1];
        float y[] = new float[n+1];
        for(int i=0; i<n+1; i++) {
            x[i] = (float) (1+0.5*i);
            y[i] = (float) Math.sqrt(1+x[i]*x[i]*x[i]);
            flowInfo += Integer.toString(i+1) + " : (" + Float.toString(x[i]) + ", " + Float.toString(y[i]) + ")\n";
        }
        float a = x[0], b = x[n];
        float dx = (b-a)/(float)n;
        float simpsonResult1 = m_simpson.simpson(y, a, b);
        float simpsonResult2 = m_simpson.simpson(y, dx);

        flowInfo += "\na : " + Float.toString(a) + "\nb : " + Float.toString(b) + "\ndx : " + Float.toString(dx) + "\n";
        flowInfo += "Simpson Result : " + Float.toString(simpsonResult1) + ", another method: " + Float.toString(simpsonResult2);

        m_txtv_simpson.setText(flowInfo);
    }
}
