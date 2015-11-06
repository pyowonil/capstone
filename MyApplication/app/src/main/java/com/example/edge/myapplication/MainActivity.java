package com.example.edge.myapplication;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
    LinearLayout linear;

    SensorInfo sensorinfo;
    Intent sensorSerivce;
    BroadcastReceiver receiver;
    boolean flag = true;

    TextView re, text;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        re = (TextView)findViewById(R.id.re);
        text = (TextView)findViewById(R.id.text);
        linear = (LinearLayout)findViewById(R.id.linear);
        //sensorinfo = new SensorInfo();
        sensorSerivce = new Intent(this, SensorService.class);
        receiver = new SensorReceiver();
        if(flag){
            try{
                IntentFilter mainFilter = new IntentFilter("MAIN");
                registerReceiver(receiver, mainFilter);
            }catch(Exception e) {

            }
        }else{
            try{
                unregisterReceiver(receiver);
                stopService(sensorSerivce);
            }catch(Exception e){

            }
        }
        //flag = !flag;
        startService(sensorSerivce);
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.linear);
        SensorView sv = new SensorView(this);
        linearLayout.addView(sv);

//        // set gps object
//        m_gps = new GpsInfo(MainActivity.this);
//
//        m_txtv_simpson = (TextView)findViewById(R.id.simpson);
//        m_txtv_wifi = (TextView)findViewById(R.id.wifiscan);
//        m_txtv_gps = (TextView)findViewById(R.id.gpsinfo);
//
//        m_btn_gpsinfo = (Button)findViewById(R.id.btn_gpsinfo);
//
//        m_btn_gpsinfo.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View arg) {
//                // gps 사용 유무 가져오기
//                if(m_gps.isGetLocation()) {
//                    double latitude, longitude;
//                    latitude = m_gps.getLatitude();
//                    longitude = m_gps.getLongitude();
//                    String gpsinfo = "Latitude   : " + Double.toString(latitude) + "\nLongitude : " + Double.toString(longitude);
//                    m_txtv_gps.setText(gpsinfo);
//                } else {
//                    m_gps.showSettingsAlert();
//                    String gpsinfo = "Latitude   : " + "Error" + "\nLongitude : " + "Error";
//                    m_txtv_gps.setText(gpsinfo);
//                }
//            }
//        });
//
//        String flowInfo = "";
//        int n = 6;
//        float x[] = new float[n+1];
//        float y[] = new float[n+1];
//        for(int i=0; i<n+1; i++) {
//            x[i] = (float) (1+0.5*i);
//            y[i] = (float) Math.sqrt(1+x[i]*x[i]*x[i]);
//            flowInfo += Integer.toString(i+1) + " : (" + Float.toString(x[i]) + ", " + Float.toString(y[i]) + ")\n";
//        }
//        float a = x[0], b = x[n];
//        float dx = (b-a)/(float)n;
//        float simpsonResult1 = simpson(y, a, b);
//        float simpsonResult2 = simpson(y, dx);
//
//        flowInfo += "\na : " + Float.toString(a) + "\nb : " + Float.toString(b) + "\ndx : " + Float.toString(dx) + "\n";
//        flowInfo += "Simpson Result : " + Float.toString(simpsonResult1) + ", " + Float.toString(simpsonResult2);
//
//        m_txtv_simpson.setText(flowInfo);
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

    @Override
    public void onDestroy(){
        super.onDestroy();
//        stopService(new Intent(this, SensorService.class));
    }

    int i= 0;

    public class SensorReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("RECEIVER : ", "HIHIHIHIHIHIHIHIHIHIHIHIHIHHIHIHI");
            sensorinfo = intent.getParcelableExtra("sensorData");
            Bundle bundle = getIntent().getExtras();
            if(bundle != null){
                text.setText("received data ok");
            }else{
                text.setText("received data null");
            }
            //sensorinfo = bundle.getParcelable("sensorData");
//            i++;
//            text.setText("received data(count) : " + i);
            //acc = intent.getFloatArrayExtra("sensorData");
            //sensorinfo.setAccSensor(acc[0],acc[1],acc[2]);
            //Toast.makeText(getApplicationContext(),"Walll....",1).show();
        }
    }

    // temporary test class
    class SensorView extends View{

        float x = 0;
        float y = 0;
        private int t = 0;
        float y2 = 0;
        float q = 20;
        private Paint paint;
        private float accX[], accY[], accZ[];
        float intvls = 0;

        public SensorView(Context context){
            super(context);
            paint = new Paint();
        }

        private void drawAcc(Canvas canvas, float accX[], float accY[], float accZ[]){
            paint.setColor(Color.GRAY);
            paint.setStrokeWidth(5);
            canvas.drawLine(0, 0, 0, y, paint);
            canvas.drawLine(0, y2, x, y2, paint);
            canvas.drawLine(0, y2*3, x, y2*3, paint);
            canvas.drawLine(0, y2*5, x, y2*5, paint);
            paint.setStrokeWidth(5);

            for(int i=0; i<100; i++){
                int m = (t-i);
                if(m<0) m += t;
                int nm = m+1;
                if(nm>=x) nm -= t;
                paint.setColor(Color.RED);
                canvas.drawLine((i + 1) * intvls, y2 + accX[m] * q, i * intvls, y2 + accX[nm] * q, paint);
                paint.setColor(Color.BLUE);
                canvas.drawLine((i+1)*intvls, y2*3 + accY[m]*q, i*intvls, y2*3+accY[nm]*q, paint);
                paint.setColor(Color.GREEN);
                canvas.drawLine((i+1)*intvls, y2*5 + accZ[m]*q, i*intvls, y2*5+accZ[nm]*q, paint);
            }
//            t++;
//            if(t>=x) t = 0;
        }

        @Override
        protected void onDraw(Canvas canvas){
            //synchronized (this){
            super.onDraw(canvas);
            paint.setAntiAlias(true);
            if(x == 0){
                x = canvas.getWidth();
                y = canvas.getHeight();
                y2 = y/6;
                intvls = x/100;
            }
            if(sensorinfo != null){
                re.setText("RECEIVE OK");
                accX = sensorinfo.getAccX();
                accY = sensorinfo.getAccY();
                accZ = sensorinfo.getAccZ();
                drawAcc(canvas, accX, accY, accZ);
            }else{
                re.setText("NOT RECEIVED");
            }

            invalidate();
        }
    }
}
