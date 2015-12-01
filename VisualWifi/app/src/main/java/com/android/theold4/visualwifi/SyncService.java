package com.android.theold4.visualwifi;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;

/**
 * Created by Jang on 2015-12-01.
 *  Data만 동기화
 *  C :  - LocalData  + WifiData
 *  S :  + WifiData
 *
 */
public class SyncService extends Service{

    Thread SendDataThread, RecvDataThread, RecvDeviceThread;
    DBManager helper;
    SQLiteDatabase db_w, db_r;

    private String mac ;
    private float lat, lon;
    private String ssid, pw;
    private int rssi, date, time;

    public void onCreate(){
        super.onCreate();
        Log.i("Sync", "startService");
        helper = new DBManager(this);
        try {
            db_r = helper.getReadableDatabase();
            db_w = helper.getWritableDatabase();
            //데이터베이스 객체를 얻기 위하여 getWritableDatabse()를 호출
        } catch (SQLiteException e) {

        }
        SendDataThread SDThread = new SendDataThread();
        RecvDataThread RDThread = new RecvDataThread();
        RecvDeviceThread RDeThread = new RecvDeviceThread();
        SDThread.start();
        RDThread.start();
        RDeThread.start();
    }
    class SendDataThread extends Thread {
        public void run() {
            try {
                db_r = helper.getReadableDatabase();
                db_w = helper.getWritableDatabase();
                //데이터베이스 객체를 얻기 위하여 getWritableDatabse()를 호출
            } catch (SQLiteException e) {
            }

            try {
                String IP = "165.246.43.250";
                int PORT = 5555;
                Socket s = new Socket(IP, PORT);

                //PrintWriter out = new PrintWriter(new BufferedWriter(
                //       new OutputStreamWriter(s.getOutputStream(), "utf-8")), true);
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                DataInputStream dis = new DataInputStream(s.getInputStream());

                dos.writeUTF("WifiData");
                String sql = "SELECT * FROM LocalData;";
                Cursor c = db_r.rawQuery(sql, null);
                while (c.moveToNext()) {
                    mac = c.getString(c.getColumnIndex("MAC"));
                    lat = c.getFloat(c.getColumnIndex("Latitude"));
                    lon = c.getFloat(c.getColumnIndex("Longitude"));
                    ssid = c.getString(c.getColumnIndex("SSID"));
                    rssi = c.getInt(c.getColumnIndex("RSSI"));
                    date = c.getInt(c.getColumnIndex("DATE"));
                    time = c.getInt(c.getColumnIndex("TIME"));

                    dos.writeUTF("" + mac + " " + lat + " " + lon + " " + ssid + " " + rssi + " " + date + " " + time);
                }
                db_r.execSQL("delete from LocalData;");
            } catch (Exception e) {
            }
        }
    }
    class RecvDataThread extends Thread{
        String line;
        String IP = "165.246.43.250";
        int PORT = 5555;
        public void run(){
            try {
                Socket s = new Socket(IP, PORT);
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                DataInputStream dis = new DataInputStream(s.getInputStream());

                dos.writeUTF("WifiData_down");
                while(true) {
                    try {
                        line = dis.readUTF();
                    }catch(IOException ioe){
                        break;
                    }
                    StringTokenizer st = new StringTokenizer(line, " ");

                    String _mac = st.nextToken();
                    String _lat = st.nextToken();
                    String _lon = st.nextToken();
                    String _ssid = st.nextToken();
                    String _rssi = st.nextToken();
                    String _date = st.nextToken();
                    String _time = st.nextToken();

                    db_w.execSQL("replace into WifiData values "
                            + "('" + _mac + "', '" + _lat + "', '" + _lon + "', '" + _ssid + "', '" + _rssi + "', '"
                            + _date + "', '" + _time + "');");

                    Log.i("DB", "'" + _mac + "', '" + _lat + "', '" + _lon + "', '" + _ssid + "', '" + _rssi + "', '"
                            + _date + "', '" + _time);
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    class RecvDeviceThread extends Thread{
        String line;
        String IP = "165.246.43.250";
        int PORT = 5555;
        public void run(){
            try {
                Socket s = new Socket(IP, PORT);
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                DataInputStream dis = new DataInputStream(s.getInputStream());

                dos.writeUTF("WifiDevice");
                while( true ){
                    try {
                        line = dis.readUTF();
                    }catch(IOException ioe){
                        break;
                    }

                    StringTokenizer st = new StringTokenizer(line, " ");

                    String _mac = st.nextToken();
                    String _lat = st.nextToken();
                    String _lon = st.nextToken();
                    String _ssid = st.nextToken();
                    String _pw = st.nextToken();
                    String _date = st.nextToken();
                    String _time = st.nextToken();

                    db_w.execSQL("replace into WifiDevice values "
                            + "('" + _mac + "', '" + _lat + "', '" + _lon + "', '" + _ssid + "', '" + _pw + "', '"
                            + _date + "', '" + _time + "');");

                    Log.i("DB", "'" + _mac + "', '" + _lat + "', '" + _lon + "', '" + _ssid + "', '" +_pw+ "', '"
                            + _date + "', '" + _time);
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public IBinder onBind(Intent arg0){
        return null;
    }
}
