package com.example.edge.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // text view
    private TextView m_txtv_simpson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_txtv_simpson = (TextView)findViewById(R.id.simpson);

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
        float simpsonResult1 = simpson(y, a, b);
        float simpsonResult2 = simpson(y, dx);

        flowInfo += "\na : " + Float.toString(a) + "\nb : " + Float.toString(b) + "\ndx : " + Float.toString(dx) + "\n";
        flowInfo += "Simpson Result : " + Float.toString(simpsonResult1) + ", " + Float.toString(simpsonResult2);

        m_txtv_simpson.setText(flowInfo);
    }

    public float simpson(float[]y, float a, float b) {
        return simpson(y, (b-a)/(y.length-1));
    }
    public float simpson(float[] y, float dx) {
        assert(y.length > 2 && y.length % 2 == 1);
        int n = y.length;
        float result = y[0] + y[n-1];
        float oddOrders = 0, evenOrders = -y[0];
        for(int i=1; i<n-1; i+=2) {
            oddOrders += y[i];
            evenOrders += y[i-1];
        }
        oddOrders *= 4;
        evenOrders *= 2;
        result += oddOrders + evenOrders;
        result = (float) ((result * dx)/3.0);

        return result;
    }
}
