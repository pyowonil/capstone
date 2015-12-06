package com.capstone.theold4.visualwifi;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class Guide extends AppCompatActivity implements OnClickListener{
    Button btn_finish;
    ScrollView scr_guide;
    TextView txt_guide;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        btn_finish = (Button) findViewById(R.id.btn_finish);
        scr_guide = (ScrollView) findViewById(R.id.scr_guide);
        txt_guide = (TextView) findViewById(R.id.txtv_guide);

        btn_finish.setOnClickListener(this);

        display();
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btn_finish) {
            Log.i("btn", "OnClick() btn_finish()");
            finish();
        }
    }

    public void display(){
        String numberLinesString = "";
        for(int i=1; i<=150; i++) {
            numberLinesString += Integer.toString(i) + "\n";
        }
        numberLinesString += "end";
        txt_guide.setText(numberLinesString);
    }
}
