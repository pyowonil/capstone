package com.capstone.theold4.visualwifi;


import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.StringTokenizer;

public class wifi_setting_synchronize extends Service {
    private database_manager mDatabaseManager;
    private SQLiteDatabase mDatabaseWrite, mDatabaseRead;
    private int mStartId;

    // = = = = = = = = = = 서비스 쓰레드 = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    private Thread mThread;
    private Runnable mRun = new Runnable() {
        @Override
        public void run() {
            try{
                // 서버에 연결
                Log.i("[SYNCHRONIZE]", "Connect to server start");
                final String ServerIP = getResources().getString(R.string.server_ip);
                final int ServerPORT = getResources().getInteger(R.integer.server_port);
                final int TIMEOUT = getResources().getInteger(R.integer.connect_timeout);

                // ****************************************
                // Server Wifi Data -> Client Wifi Data
                // ****************************************
                Log.i("[SYNCHRONIZE]", "[Receive data from server] [start]");
                Socket socket1 = new Socket();
                SocketAddress socketAddress1 = new InetSocketAddress(ServerIP, ServerPORT);
                socket1.connect(socketAddress1, TIMEOUT);
                if(!socket1.isConnected()) {
                    Log.i("[SYNCHRONIZE]", "Connect socket1 fail");
                } else {
                    // - - - - - - - - - - receive data from server to client - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                    DataOutputStream dataOutputStream = new DataOutputStream(socket1.getOutputStream());
                    DataInputStream dataInputStream = new DataInputStream(socket1.getInputStream());

                    dataOutputStream.writeUTF("WifiData_down");
                    String data;
                    String query;
                    while (true) {
                        try {
                            data= dataInputStream.readUTF();
                        } catch (IOException ioe) {
                            break;
                        }
                        if(data.equals("END"))break;

                        StringTokenizer token = new StringTokenizer(data, "\t");
                        String mac = token.nextToken();
                        String lat = token.nextToken();
                        String lon = token.nextToken();
                        String ssid = token.nextToken();
                        String pw = token.nextToken();
                        String date = token.nextToken();
                        String time = token.nextToken();

                        query = "REPLACE INTO WifiData VALUES " + "('" + mac + "', '" + lat + "', '" + lon +
                                "', '" + ssid + "', '" + pw + "', '" + date + "', '" + time + "');";
                        mDatabaseWrite.execSQL(query);
                    }

                }
                socket1.close();
                Log.i("[SYNCHRONIZE]", "[Receive data from server] [finish]");

                // ****************************************
                // Server Wifi Device -> Client Wifi Device
                // ****************************************
                Log.i("[SYNCHRONIZE]", "[Receive device from server] [start]");
                Socket socket2 = new Socket();
                SocketAddress socketAddress2 = new InetSocketAddress(ServerIP, ServerPORT);
                socket2.connect(socketAddress2, TIMEOUT);
                if(!socket2.isConnected()) {
                    Log.i("[SYNCHRONIZE]", "Connect socket2 fail");
                } else {
                    DataOutputStream dataOutputStream = new DataOutputStream(socket2.getOutputStream());
                    DataInputStream dataInputStream = new DataInputStream(socket2.getInputStream());

                    dataOutputStream.writeUTF("WifiDevice_down");
                    String data;
                    String query;
                    while (true) {
                        try {
                            data = dataInputStream.readUTF();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                            break;
                        }
                        if(data.equals("END"))break;
                        StringTokenizer token = new StringTokenizer(data, "\t");
                        String mac = token.nextToken();
                        String lat = token.nextToken();
                        String lon = token.nextToken();
                        String ssid = token.nextToken();
                        String date = token.nextToken();
                        String time = token.nextToken();

                        query = "REPLACE INTO WifiDevice VALUES " + "('" + mac + "', '" + lat + "', '" + lon +
                                "', '" + ssid + "', '" + date + "', '" + time + "');";
                        mDatabaseWrite.execSQL(query);
                    }

                }
                socket2.close();
                Log.i("[SYNCHRONIZE]", "[Receive device from server] [finish]");
                Thread.sleep(5000);

                // *******************
                // Compute AP Location
                // *******************
                ComputeAP();




                // ****************************************
                // Client Local Data  ->  Server Wifi Data
                // ****************************************
                Socket socket3 = new Socket();
                SocketAddress socketAddress3 = new InetSocketAddress(ServerIP, ServerPORT);
                socket3.connect(socketAddress3, TIMEOUT);
                if(!socket3.isConnected()) {
                    Log.i("[SYNCHRONIZE]", "Connect socket3 fail");
                } else {
                    DataOutputStream dataOutputStream3 = new DataOutputStream(socket3.getOutputStream());
                    DataInputStream dataInputStream3 = new DataInputStream(socket3.getInputStream());

                    // 사용할 변수들
                    String query3 = "SELECT * FROM LocalData;";
                    Cursor cursor = mDatabaseRead.rawQuery(query3, null);
                    int id_mac, id_latitude, id_longitude, id_ssid, id_rssi, id_date, id_time;
                    id_mac = cursor.getColumnIndex("MAC");
                    id_latitude = cursor.getColumnIndex("Latitude");
                    id_longitude = cursor.getColumnIndex("Longitude");
                    id_ssid = cursor.getColumnIndex("SSID");
                    id_rssi = cursor.getColumnIndex("RSSI");
                    id_date = cursor.getColumnIndex("DATE");
                    id_time = cursor.getColumnIndex("TIME");
                    String data;
                    // - - - - - - - - - - send data from client to server - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                    Log.i("[SYNCHRONIZE]", "[Send data to server] [start]");
                    dataOutputStream3.writeUTF("WifiData");
                    while (cursor.moveToNext()) {
                        data = "" + cursor.getString(id_mac) + "\t" + cursor.getFloat(id_latitude) + "\t" + cursor.getFloat(id_longitude) +
                                "\t" + cursor.getString(id_ssid) + "\t" + cursor.getInt(id_rssi) + "\t" + cursor.getInt(id_date) +
                                "\t" + cursor.getInt(id_time);
                        dataOutputStream3.writeUTF(data);

                        mDatabaseWrite.execSQL("Replace into WifiData Values ('" + cursor.getString(id_mac) + "', '" + cursor.getFloat(id_latitude) +
                                "', '" + cursor.getFloat(id_longitude) + "', '" + cursor.getString(id_ssid) + "', '" + cursor.getInt(id_rssi) + "', '" +
                                cursor.getInt(id_date) + "', '" + cursor.getInt(id_time) + "');");

                    }
                    mDatabaseRead.execSQL("DELETE FROM LocalData");
                    Log.i("[SYNCHRONIZE]", "[Send data to server] [finish]");

                }
                socket3.close();

                // ****************************************
                // Client Local Device  ->  Server Wifi Device
                // ****************************************
                Socket socket4 = new Socket();
                SocketAddress socketAddress4 = new InetSocketAddress(ServerIP, ServerPORT);
                socket4.connect(socketAddress4, TIMEOUT);
                if(!socket4.isConnected()) {
                    Log.i("[SYNCHRONIZE]", "Connect socket4 fail");
                } else {
                    DataOutputStream dataOutputStream4 = new DataOutputStream(socket4.getOutputStream());
                    DataInputStream dataInputStream4 = new DataInputStream(socket4.getInputStream());

                    // 사용할 변수들
                    String query4 = "SELECT * FROM LocalDevice;";
                    Cursor cursor = mDatabaseRead.rawQuery(query4, null);
                    int id_mac, id_latitude, id_longitude, id_ssid, id_date, id_time;
                    id_mac = cursor.getColumnIndex("MAC");
                    id_latitude = cursor.getColumnIndex("Latitude");
                    id_longitude = cursor.getColumnIndex("Longitude");
                    id_ssid = cursor.getColumnIndex("SSID");
                    id_date = cursor.getColumnIndex("DATE");
                    id_time = cursor.getColumnIndex("TIME");
                    String data;
                    // - - - - - - - - - - send device from client to server - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                    Log.i("[SYNCHRONIZE]", "[Send device to server] [start]");
                    dataOutputStream4.writeUTF("WifiDevice");
                    while (cursor.moveToNext()) {
                        data = "" + cursor.getString(id_mac) + "\t" + cursor.getFloat(id_latitude) + "\t" + cursor.getFloat(id_longitude) +
                                "\t" + cursor.getString(id_ssid) + "\t" + cursor.getInt(id_date) +
                                "\t" + cursor.getInt(id_time);
                        dataOutputStream4.writeUTF(data);

                        mDatabaseWrite.execSQL("Replace into WifiDevice Values ('" + cursor.getString(id_mac)+"', '"+ cursor.getFloat(id_latitude)+
                                            "', '" + cursor.getFloat(id_longitude) + "', '"+ cursor.getString(id_ssid)+"', '"+cursor.getInt(id_date) +"', '"+
                                            cursor.getInt(id_time)+ "');" );
                    }
                    mDatabaseRead.execSQL("DELETE FROM LocalDevice");
                    Log.i("[SYNCHRONIZE]", "[Send Device to server] [finish]");

                }
                socket4.close();


                // ****************************************
                // Server Wifi Share -> Client Wifi Share
                // ****************************************
                Log.i("[SYNCHRONIZE]", "[Receive share from server] [start]");
                Socket socket5 = new Socket();
                SocketAddress socketAddress5 = new InetSocketAddress(ServerIP, ServerPORT);
                socket5.connect(socketAddress5, TIMEOUT);
                if(!socket5.isConnected()) {
                    Log.i("[SYNCHRONIZE]", "Connect socket4 fail");
                } else {
                    DataOutputStream dataOutputStream = new DataOutputStream(socket5.getOutputStream());
                    DataInputStream dataInputStream = new DataInputStream(socket5.getInputStream());

                    dataOutputStream.writeUTF("Share_down");
                    String data;
                    String query;
                    while (true) {
                        try {
                            data = dataInputStream.readUTF();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                            break;
                        }
                        if(data.equals("END"))break;
                        StringTokenizer token = new StringTokenizer(data, "\t");
                        String mac = token.nextToken();
                        String ssid = token.nextToken();
                        String pw = token.nextToken();
                        String capability = token.nextToken();
                        String date = token.nextToken();
                        String time = token.nextToken();

                        query = "REPLACE INTO WifiDevice VALUES " + "('" + mac + "', '" + ssid + "', '" + pw +
                                "', '" + capability + "', '" + date + "', '" + time + "');";
                        mDatabaseWrite.execSQL(query);
                    }

                }
                socket5.close();
                Log.i("[SYNCHRONIZE]", "[Receive share from server] [finish]");




            }catch(Exception e) {
                e.printStackTrace();
            }finally {
                stopSelf(mStartId);
            }
        }
    };
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 서비스 쓰레드 = = = = = = = = = =

    // = = = = = = = = = = 액티비티 시작 (onCreate) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    public void onCreate() {
        super.onCreate();
        mDatabaseManager = new database_manager(this);
        try {
            mDatabaseWrite = mDatabaseManager.getWritableDatabase();
            mDatabaseRead = mDatabaseManager.getReadableDatabase();
        } catch (SQLiteException e) {
        }
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 액티비티 시작 (onCreate) = = = = = = = = = =

    public void ComputeAP(){
        database_manager helper;
        SQLiteDatabase db_r, db_w;

        final double quarterPI = Math.PI/4;
        final double dir_EES = -Math.PI / 8;
        final double dir_EEN = dir_EES + quarterPI;
        final double dir_NNE = dir_EEN + quarterPI;
        final double dir_NNW = dir_NNE + quarterPI;
        final double dir_WWN = dir_NNW + quarterPI;
        final double dir_WWS = -dir_WWN;
        final double dir_SSW = dir_WWS + quarterPI;
        final double dir_SSE = dir_SSW + quarterPI;

        helper = new database_manager(this);
        float EEx=0, WWx=0, SSx=0, NNx=0 , ENx=0, WNx=0, ESx=0, WSx=0; // 8방위 x좌표
        float EEy=0, WWy=0, SSy=0, NNy=0 , ENy=0, WNy=0, ESy=0, WSy=0; // 8방위 y좌표
        float EEd=0, WWd=0, SSd=0, NNd=0 , ENd=0, WNd=0, ESd=0, WSd=0; // 8방위까지의 max 거리
        float CCx=0, CCy=0, tempX=0, tempY=0, tempD=0;

        double angle;

        try
        {
            db_w = helper.getWritableDatabase();
            db_r = helper.getReadableDatabase();

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
    }



    // = = = = = = = = = = 서비스 시작 (onStartCommand) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;
        mThread = new Thread(mRun);
        mThread.start();
        // < START_STICKY >
        // 메모리부족이나 기타 상황에서 시스템이 강제로 service를 종료된후
        // service가 재시작될때 null Intent가 담긴 onStartCommand() 콜백함수가 실행된다.
        // 이 경우 null Intent로 호출때의 경우를 처리해줘야 합니다
        // < START_NOT_STICKY >
        // 이 경우는 프로세스가 강제로 종료되었을 경우 재시작하지 않고 종료된 상태로 남게 됩니다.
        // 예를 들면 매 15분마다 네트워크 체크를 하는 service가 강제로 종료되었을경우
        // 15분후에 자동적으로 다시 service가 실행되므로 재시작하지 않아도 되는 경우입니다.
        // < START_REDELIVER_INTENT >
        // 이 경우에는 프로세스가 강제로 종료되었을 경우 Intent가 다시 전달되어 재시작합니다.
        // 단, 여러차레 시도후 작업이 종료되지 않으면 service는 재시작 되지 않습니다.
        // 반드시 실행되어야 하는 service에 해당이 됩니다.
        return START_STICKY;
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 서비스 시작 (onStartCommand) = = = = = = = = = =

    // = = = = = = = = = = 서비스 종료 (onDestroy) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 서비스 종료 (onDestroy) = = = = = = = = = =

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
