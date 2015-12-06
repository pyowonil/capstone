package com.capstone.theold4.visualwifi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.os.Handler;


public class Intro extends Activity {

    Handler h;

    @Override
    public void onCreate(Bundle savedIntanceState) {
        super.onCreate(savedIntanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);
        h = new Handler();
        h.postDelayed(irun, 1000);
    }
    Runnable irun = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(getApplicationContext(), visual_wifi_map.class);
            startActivity(intent);
            finish();

            //fade in -> fade out
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        h.removeCallbacks(irun);
    }
}
