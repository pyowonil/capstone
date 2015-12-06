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
                Thread.sleep(5000);

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
                    }
                    mDatabaseRead.execSQL("DELETE FROM LocalData");
                    Log.i("[SYNCHRONIZE]", "[Send data to server] [finish]");

                }
                socket3.close();


                // ****************************************
                // Server Wifi Share -> Client Wifi Share
                // ****************************************
                Log.i("[SYNCHRONIZE]", "[Receive share from server] [start]");
                Socket socket4 = new Socket();
                SocketAddress socketAddress4 = new InetSocketAddress(ServerIP, ServerPORT);
                socket4.connect(socketAddress4, TIMEOUT);
                if(!socket4.isConnected()) {
                    Log.i("[SYNCHRONIZE]", "Connect socket4 fail");
                } else {
                    DataOutputStream dataOutputStream = new DataOutputStream(socket4.getOutputStream());
                    DataInputStream dataInputStream = new DataInputStream(socket4.getInputStream());

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
                socket4.close();
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
