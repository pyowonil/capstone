package com.android.theold4.visualwifi;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.IBinder;

/**
 * Created by Jang on 2015-12-02.
 * 앱을 실행하자마자 AP 위치 계산하는 서비스
 *
 *
 *
 */
public class ComputeAPLocationService extends Service implements Runnable {
    Thread myThread;
    DBManager helper;
    SQLiteDatabase db;
    static final double tan22_5 = Math.tan(22.5);
    static final double tan67_5 = Math.tan(67.5);
    static final double tan112_5 = Math.tan(112.5);
    static final double tan157_5 = Math.tan(157.5);
    static final double tan202_5 = Math.tan(202.5);
    static final double tan247_5 = Math.tan(247.5);
    static final double tan292_5 = Math.tan(292_5);
    static final double tan337_5 = Math.tan(337_5);

    public void onCreate() {
        super.onCreate();
        myThread = new Thread(this);
        myThread.start();
    }
    public void run(){
        helper = new DBManager(this);
        float EEx, WWx, SSx, NNx , ENx, WNx, ESx, WSx; // 8방위 x좌표
        float EEy, WWy, SSy, NNy , ENy, WNy, ESy, WSy; // 8방위 y좌표
        float EEd, WWd, SSd, NNd , ENd, WNd, ESd, WSd; // 8방위까지의 max 거리
        float CCx, CCy, tempX, tempY, tempD;



        try {
            db = helper.getWritableDatabase();
            db = helper.getReadableDatabase();
            //데이터베이스 객체를 얻기 위하여 getWritableDatabse()를 호출
        } catch (SQLiteException e) {
        }


        String sql1 = "SELECT MAC, Latitude, Longitude FROM LocalDevice;";
        Cursor c = db.rawQuery(sql1, null);
        while(c.moveToNext()){
            String _mac = c.getString( c.getColumnIndex("MAC"));
            CCx = c.getFloat(c.getColumnIndex("Longitude"));
            CCy = c.getFloat(c.getColumnIndex("Latitude"));

            String sql2 = "SELECT Latitude, Longitude FROM LocalData";
            Cursor c2 = db.rawQuery(sql2,null);
            while(c2.moveToNext()){
                tempY = c2.getFloat(c2.getColumnIndex("lat"));
                tempX = c2.getFloat(c2.getColumnIndex("lon"));

                // 중심으로의 상대적 위치 및 거리
                tempY -= CCy;
                tempX -= CCx;
                tempD = tempX*tempX + tempY*tempY;
                // 8방향 잡기






            }



        }






    }
    public IBinder onBind(Intent arg0){
        return null;
    }
}
