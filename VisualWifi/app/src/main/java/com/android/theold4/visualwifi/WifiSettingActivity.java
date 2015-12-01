package com.android.theold4.visualwifi;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Created by Jang on 2015-11-20.
 * 목적 : 사이드메뉴에서 Wifi 설정을 선택했을 때 동작하는 액티비티
 */
public class WifiSettingActivity extends AppCompatActivity implements View.OnClickListener {
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_setting);

        TextView textView1 = (TextView)findViewById(R.id.textView);
        TextView textView2 = (TextView)findViewById(R.id.textView2);
        Button buttonUpload = (Button)findViewById(R.id.button_upload);
        Button buttonDownload = (Button)findViewById(R.id.button_download);
        Switch switchAuto = (Switch)findViewById(R.id.switch_auto);
        final Button buttonWifiList = (Button)findViewById(R.id.button_wifi_list);
        Button buttonExitSetting = (Button)findViewById(R.id.button_exit_setting);

        buttonDownload.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){     // 동기화 -> (LocalData) 업로드, (WifiData, WifiDevice) 다운로드 //  SQLite , 서버 통신
                Intent downIntent = new Intent(WifiSettingActivity.this,SyncService.class);
                downIntent.putExtra("setting", "download");
                startService(downIntent);
                setResult(RESULT_OK, downIntent);
                stopService(downIntent);
                finish();
            }
        });

        buttonUpload.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){       // 업로드 -> (LocalDevice) 업로드  리스트에서 1개씩만 선택해서 가능  // SQLite , 서버 통신

                DBManager helper = new DBManager(getApplicationContext());;
                SQLiteDatabase db_r;
                db_r = helper.getReadableDatabase();

                String sql = "SELECT * FROM WifiData;";
                Cursor c = db_r.rawQuery(sql, null);
                while(c.moveToNext()){
                    String mac = c.getString( c.getColumnIndex("MAC"));
                    float lat = c.getFloat(c.getColumnIndex("Latitude"));
                    float lon = c.getFloat(c.getColumnIndex("Longitude"));
                    String ssid = c.getString(c.getColumnIndex("SSID"));
                    int rssi = c.getInt(c.getColumnIndex("RSSI"));
                    int date = c.getInt(c.getColumnIndex("DATE"));
                    int time = c.getInt(c.getColumnIndex("TIME"));

                    Log.i("db",""+mac+" "+lat+" "+lon+" "+ssid+" "+rssi+" "+date+" "+time);
                }

                finish();
            }
        });

        switchAuto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked){
                Context context;
//                Intent intentService = new Intent(".AutoConnectService");
                Intent intentService = new Intent(WifiSettingActivity.this, AutoConnectService.class);
                if(isChecked){
                    // Wifi 자동연결 상태일때
                    buttonWifiList.setVisibility(View.INVISIBLE);
                    // Wifi 자동 연결 서비스 시작
                    startService(intentService);
                }
                else{
                    // Wifi 수동연결 상태일때
                    // Wifi 선택 버튼 보여짐
                    buttonWifiList.setVisibility(View.VISIBLE);
                    // Wifi 자동 연결 서비스 종료
                    stopService(intentService);
                }

            }
        });


        buttonWifiList.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){  // Wifi 수동연결일때 현재 연결가능한 Wifi 리스트 보여줌 // DB에 pw저장되있는 wifi를 우선순위 높게 리스트상단으로

            }
        });


        buttonExitSetting.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){    // 액티비티 종료 및 메인 화면으로 이동
                Intent exitIntent = new Intent();
                exitIntent.putExtra("setting","설정 적용완료");
                setResult(RESULT_OK, exitIntent);
                finish();
            }
        });
    }
    @Override
    public void onClick(View v) {

    }

}
