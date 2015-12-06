package com.capstone.theold4.visualwifi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.os.Handler;


public class Intro extends Activity {

    @Override
    public void onCreate(Bundle savedIntanceState) {
        super.onCreate(savedIntanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);

        Loading();
    }

    private void Loading(){
        Handler handler = new Handler(){
            public void handleMessage (Message msg){
                Intent intent = new Intent(getApplicationContext(), visual_wifi_map.class);
                startActivity(intent);
                finish();

                //fade in -> fade out
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        };
        handler.sendEmptyMessageDelayed(0, 1000);
    }
}
