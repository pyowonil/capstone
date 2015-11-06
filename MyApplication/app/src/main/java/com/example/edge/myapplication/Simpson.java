package com.example.edge.myapplication;

/**
 * Created by edge on 2015-11-06.
 */
public class Simpson {
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
