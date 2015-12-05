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
                Log.i("Resource", getResources().getString(R.string.server_ip));
                final int ServerPORT = getResources().getInteger(R.integer.server_port);
                final int TIMEOUT = getResources().getInteger(R.integer.connect_timeout);

                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(ServerIP, ServerPORT);
                socket.connect(socketAddress, TIMEOUT);
                if(!socket.isConnected()) {
                    Log.i("[SYNCHRONIZE]", "Connect to server fail");
                    // TODO 서버 연결 실패시 알림
                } else {
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    Log.i("[SYNCHRONIZE]", "Connect to server finish");

                    // 사용할 변수들
                    String query = "SELECT * FROM LocalData;";
                    Cursor cursor = mDatabaseRead.rawQuery(query, null);
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
                    Log.i("[SYNCHRONIZE]", "Send data to server start");
                    dataOutputStream.writeUTF("WifiData");
                    while (cursor.moveToNext()) {
                        data = "" + cursor.getString(id_mac) + "\t" + cursor.getFloat(id_latitude) + "\t" + cursor.getFloat(id_longitude) +
                                "\t" + cursor.getString(id_ssid) + "\t" + cursor.getInt(id_rssi) + "\t" + cursor.getInt(id_date) +
                                "\t" + cursor.getInt(id_time);
                        Log.i("dbb", data);
                        dataOutputStream.writeUTF(data);
                    }
                    mDatabaseRead.execSQL("DELETE FROM LocalData");
                    Log.i("[SYNCHRONIZE]", "Send data to server finish");

                }
                socket.close();
                Log.i("sleep", "1");
                Thread.sleep(5000);
                Log.i("sleep", "2");

                Socket socket2 = new Socket();
                SocketAddress socketAddress2 = new InetSocketAddress(ServerIP, ServerPORT);
                socket2.connect(socketAddress2, TIMEOUT);
                if(!socket2.isConnected()) {
                    Log.i("[SYNCHRONIZE]", "Connect to server fail");
                    // TODO 서버 연결 실패시 알림
                } else {
                    // - - - - - - - - - - receive data from server to client - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                    DataOutputStream dataOutputStream = new DataOutputStream(socket2.getOutputStream());
                    DataInputStream dataInputStream = new DataInputStream(socket2.getInputStream());
                    Log.i("[SYNCHRONIZE]", "Receive data from server start");
                    dataOutputStream.writeUTF("WifiData_down");
                    String data;
                    String query;
                    while (true) {
                        try {
                            data= dataInputStream.readUTF();
                        } catch (IOException ioe) {
                            break;
                        }

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
                    Log.i("[SYNCHRONIZE]", "Receive data from server finish");
                }
                socket2.close();
                Socket socket3 = new Socket();
                SocketAddress socketAddress3 = new InetSocketAddress(ServerIP, ServerPORT);
                socket3.connect(socketAddress3, TIMEOUT);
                if(!socket3.isConnected()) {
                    Log.i("[SYNCHRONIZE]", "Connect to server fail");
                    // TODO 서버 연결 실패시 알림
                } else {
                    DataOutputStream dataOutputStream = new DataOutputStream(socket3.getOutputStream());
                    DataInputStream dataInputStream = new DataInputStream(socket3.getInputStream());
                    Log.i("[SYNCHRONIZE]", "Connect to server finish");
                    // - - - - - - - - - - receive device data from server to client - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                    Log.i("[SYNCHRONIZE]", "Receive device data from server start");
                    dataOutputStream.writeUTF("WifiDevice");
                    String data;
                    String query;
                    while (true) {
                        try {
                            data = dataInputStream.readUTF();
                        } catch (IOException ioe) {
                            break;
                        }

                        StringTokenizer token = new StringTokenizer(data, "\t");
                        String mac = token.nextToken();
                        String lat = token.nextToken();
                        String lon = token.nextToken();
                        String ssid = token.nextToken();
                        String pw = token.nextToken();
                        String date = token.nextToken();
                        String time = token.nextToken();

                        query = "REPLACE INTO WifiDevice VALUES " + "('" + mac + "', '" + lat + "', '" + lon +
                                "', '" + ssid + "', '" + pw + "', '" + date + "', '" + time + "');";
                        mDatabaseWrite.execSQL(query);
                    }
                    Log.i("[SYNCHRONIZE]", "Receive device data from server finish");

                    socket3.close();
                }
            }catch(Exception e) {
                e.printStackTrace();
            }finally {
                // TODO 종료전 마지막 작업
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
            // TODO 데이터베이스 에러처리 필요
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
