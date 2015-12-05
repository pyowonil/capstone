package com.capstone.theold4.visualwifi;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Jang on 2015-12-02.
 * 앱을 실행하자마자 AP 위치 계산하는 서비스
 *
 *
 *
 */

public class ComputeAPLocationService extends Service implements Runnable {
    Thread myThread;
    database_manager helper;
    SQLiteDatabase db_r, db_w;

    static final double quarterPI = Math.PI/4;
    static final double dir_EES = -Math.PI / 8;
    static final double dir_EEN = dir_EES + quarterPI;
    static final double dir_NNE = dir_EEN + quarterPI;
    static final double dir_NNW = dir_NNE + quarterPI;
    static final double dir_WWN = dir_NNW + quarterPI;
    static final double dir_WWS = -dir_WWN;
    static final double dir_SSW = dir_WWS + quarterPI;
    static final double dir_SSE = dir_SSW + quarterPI;

    String TAG = "CAPL";

    public void onCreate() {
        super.onCreate();
        myThread = new Thread(this);
        myThread.start();
    }
    public void run(){
        helper = new database_manager(this);
        float EEx=0, WWx=0, SSx=0, NNx=0 , ENx=0, WNx=0, ESx=0, WSx=0; // 8방위 x좌표
        float EEy=0, WWy=0, SSy=0, NNy=0 , ENy=0, WNy=0, ESy=0, WSy=0; // 8방위 y좌표
        float EEd=0, WWd=0, SSd=0, NNd=0 , ENd=0, WNd=0, ESd=0, WSd=0; // 8방위까지의 max 거리
        float CCx=0, CCy=0, tempX=0, tempY=0, tempD=0;

        double angle;

        try {
            db_w = helper.getWritableDatabase();
            db_r = helper.getReadableDatabase();
            //데이터베이스 객체를 얻기 위하여 getWritableDatabse()를 호출
        } catch (SQLiteException e) {
        }

        try
        {
            String sql1 = "SELECT MAC, Latitude, Longitude FROM LocalDevice;";
            Cursor c = db_r.rawQuery(sql1, null);
            while (c.moveToNext()) {
                String _mac = c.getString(c.getColumnIndex("MAC"));
                CCx = c.getFloat(c.getColumnIndex("Longitude"));
                CCy = c.getFloat(c.getColumnIndex("Latitude"));

                String[] sql2 = {"SELECT Latitude, Longitude FROM LocalData WHERE MAC = '" + _mac + "';",
                        "SELECT Latitude, Longitude FROM WifiData WHERE MAC = '" + _mac + "';"};
                for (int sq = 0; sq < 2; sq++) {
                    Cursor c2 = db_r.rawQuery(sql2[sq], null);
                    while (c2.moveToNext()) {
                        tempY = c2.getFloat(c2.getColumnIndex("Latitude"));
                        tempX = c2.getFloat(c2.getColumnIndex("Longitude"));

                        // 중심으로의 상대적 위치 및 거리
                        tempY -= CCy;
                        tempX -= CCx;
                        tempD = tempX * tempX + tempY * tempY;

                        // 8방향  8점 잡기
                        if (tempX == 0 && tempY == 0) { // 센터이면
                            continue;
                        } else {
                            angle = Math.atan2(tempY, tempX);
                            if (angle >= dir_WWN) { // 서 (절반)
                                if (tempD > WWd) {
                                    WWd = tempD;
                                    WWx = tempX;
                                    WWy = tempY;
                                }
                            } else if (angle >= dir_NNW) { // 북서
                                if (tempD > WNd) {
                                    WNd = tempD;
                                    WNx = tempX;
                                    WNy = tempY;
                                }
                            } else if (angle >= dir_NNE) {  // 북
                                if (tempD > NNd) {
                                    NNd = tempD;
                                    NNx = tempX;
                                    NNy = tempY;
                                }
                            } else if (angle >= dir_EEN) {  // 북동
                                if (tempD > ENd) {
                                    ENd = tempD;
                                    ENx = tempX;
                                    ENy = tempY;
                                }
                            } else if (angle >= dir_EES) {  // 동
                                if (tempD > EEd) {
                                    EEd = tempD;
                                    EEx = tempX;
                                    EEy = tempY;
                                }
                            } else if (angle >= dir_SSE) {  // 남동
                                if (tempD > ESd) {
                                    ESd = tempD;
                                    ESx = tempX;
                                    ESy = tempY;
                                }
                            } else if (angle >= dir_SSW) {  // 남
                                if (tempD > SSd) {
                                    SSd = tempD;
                                    SSx = tempX;
                                    SSy = tempY;
                                }
                            } else if (angle >= dir_WWS) {  // 남서
                                if (tempD > WSd) {
                                    WSd = tempD;
                                    WSx = tempX;
                                    WSy = tempY;
                                }
                            } else {   //  서 (나머지 절반)
                                if (tempD > WWd) {
                                    WWd = tempD;
                                    WWx = tempX;
                                    WWy = tempY;
                                }
                            }
                        }  // if else 문, 8점 비교 대입
                    } // while문 Local, Wifi 테이블 8점 잡기
                }  // for문 end 8점 잡기 완료 ( DB 모두 검색 완료 )

                // 중점 잡기 ( AP 위치 잡기 )
                CCx = CCx + (NNx + SSx + EEx + WWx + WNx + WSx + ENx + ESx) / 8;
                CCy = CCy + (NNy + SSy + EEy + WWy + WNy + WSy + ENy + ESy) / 8;

                // localDevice 테이블 업데이트하기
                String sql3 = "UPDATE LocalDevice " +
                        "SET Latitude=" + CCy + ", Longitude="+ CCx +
                        " WHERE MAC = '" + _mac + "';";

                db_w.execSQL(sql3);

            }// while문 end  LocalDevice 1개 해당하는 루프
        }
        catch(SQLiteException sqlE){
            sqlE.printStackTrace();
        }
    }// run() end

    public IBinder onBind(Intent arg0){
        return null;
    }
}
