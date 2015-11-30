package com.android.theold4.visualwifi;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DBManager extends SQLiteOpenHelper {


    private static final String db_Local = "DBLocal.db";
    private static final int DATABASE_VERSION =1;


    /*
     *먼저 SQLiteOpenHelper클래스를 상속받은 dbHelper클래스가 정의 되어 있다. 데이터베이스 파일 이름은 "mycontacts.db"가되고,
     *데이터베이스 버전은 1로 되어있다. 만약 데이터베이스가 요청되었는데 데이터베이스가 없으면 onCreate()를 호출하여 데이터베이스
     *파일을 생성해준다.
     */

    public DBManager(Context context) {
        super(context, db_Local, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE LocalDevice (MAC TEXT PRIMARY KEY, Latitude float, Longitude float, SSID TEXT, " +
                "PW TEXT, DATE INTEGER, TIME INTEGER);");
        db.execSQL("CREATE TABLE WifiDevice (MAC TEXT PRIMARY KEY, Latitude float, Longitude float, SSID TEXT, " +
                "PW TEXT, DATE INTEGER, TIME INTEGER);");
        db.execSQL("CREATE TABLE LocalData (MAC TEXT, Latitude float, Longitude float, SSID TEXT, " +
                "RSSI INTEGER, DATE INTEGER, TIME INTEGER, CONSTRAINT data_uc UNIQUE(MAC, Latitude, Longitude ));");
        db.execSQL("CREATE TABLE WifiData (MAC TEXT, Latitude float, Longitude float, SSID TEXT, " +
                "RSSI INTEGER, DATE INTEGER, TIME INTEGER, CONSTRAINT data_uc UNIQUE(MAC, Latitude, Longitude ));");

        //db.execSQL("CREATE TABLE LocalData ( MAC TEXT PRIMARY KEY, SSID TEXT,  ")
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXITS contact");
        onCreate(db);
    }

}

    /*
    // 상수 관련
    String dbName = "WIFI.db"; // name of Database;
    String tableName = "LocalTable"; // name of Table;
    static int dbMode = Context.MODE_PRIVATE;

    // Database 관련 객체들
    static SQLiteDatabase db;

    public DBManager(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 새로운 테이블을 생성한다.
        // create table 테이블명 (컬럼명 타입 옵션);
        db.execSQL("CREATE TABLE WIFI_LIST( _id INTEGER PRIMARY KEY AUTOINCREMENT, ssid TEXT, passwd TEXT);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    // Database 생성 및 열기
    public void createDatabase(String dbName, int dbMode){
        db = openOrCreateDatabase(dbName, , null);
    }

    // Table 생성
    public void createTable(){
        String sql = "create table " + tableName + "(id integer primary key autoincrement, "+"voca text not null)";
        db.execSQL(sql);
    }

    // Table 삭제
    public void removeTable(){
        String sql = "drop table " + tableName;
        db.execSQL(sql);
    }

    // Data 추가
    public void insertData(String voca){
        String sql = "insert into " + tableName + " values(NULL, '" + voca +"');";
        db.execSQL(sql);
    }

    // Data 업데이트
    public void updateData(int index, String voca){
        String sql = "update " + tableName + " set voca = '" + voca +"' where id = "+index +";";
        db.execSQL(sql);
    }

    // Data 삭제
    public void removeData(int index){
        String sql = "delete from " + tableName + " where id = "+index+";";
        db.execSQL(sql);
    }

    // Data 읽기(꺼내오기)
    public void selectData(int index){
        String sql = "select * from " +tableName+ " where id = "+index+";";
        Cursor result = db.rawQuery(sql, null);

        // result(Cursor 객체)가 비어 있으면 false 리턴
        if(result.moveToFirst()){
            int id = result.getInt(0);
            String voca = result.getString(1);
            //Toast.makeText(this, "id= " + id + " voca=" + voca, 0).show();
        }
        result.close();
    }


    // 모든 Data 읽기
    public void selectAll(){
        String sql = "select * from " + tableName + ";";
        Cursor results = db.rawQuery(sql, null);

        results.moveToFirst();

        while(!results.isAfterLast()){
            int id = results.getInt(0);
            String voca = results.getString(1);
            //Toast.makeText(this, "index= "+id+" voca="+voca, 0).show();
            results.moveToNext();
        }
        results.close();
    }

    public String PrintData() {
        SQLiteDatabase db = getReadableDatabase();
        String str = "";

        Cursor cursor = db.rawQuery("select * from WIFI_LIST", null);
        while(cursor.moveToNext()) {
            str += cursor.getInt(0)
                    + " : id "
                    + cursor.getString(1)
                    + ", voca = "
                    + cursor.getString(2)
                    + "\n";
        }

        return str;
    }
*/
