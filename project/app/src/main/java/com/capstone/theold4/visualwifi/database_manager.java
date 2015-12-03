package com.capstone.theold4.visualwifi;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class database_manager extends SQLiteOpenHelper {
    private static final String sLocalDatabase = "DBLocal.db";
    private static final int sDatabaseVersion = 1;

    // 데이터베이스가 요청되었는데 데이터베이스가 없으면 onCreate()를 호출하여 데이터베이스 파일을 생성한다.
    database_manager(Context context) {
        super(context, sLocalDatabase, null, sDatabaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("[DATABASE]", "Create database");
        db.execSQL("CREATE TABLE LocalDevice (MAC TEXT PRIMARY KEY, Latitude float, Longitude float, SSID TEXT, " +
                "PW TEXT, DATE INTEGER, TIME INTEGER);");
        db.execSQL("CREATE TABLE WifiDevice (MAC TEXT PRIMARY KEY, Latitude float, Longitude float, SSID TEXT, " +
                "PW TEXT, DATE INTEGER, TIME INTEGER);");
        db.execSQL("CREATE TABLE LocalData (MAC TEXT, Latitude float, Longitude float, SSID TEXT, " +
                "RSSI INTEGER, DATE INTEGER, TIME INTEGER, CONSTRAINT data_uc UNIQUE(MAC, Latitude, Longitude ));");
        db.execSQL("CREATE TABLE WifiData (MAC TEXT, Latitude float, Longitude float, SSID TEXT, " +
                "RSSI INTEGER, DATE INTEGER, TIME INTEGER, CONSTRAINT data_uc UNIQUE(MAC, Latitude, Longitude ));");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}
