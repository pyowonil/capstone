package com.android.theold4.visualwifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.lang.Comparable;
import java.util.Comparator;
/**
 * Created by Jang on 2015-11-22.
 * 자동연결을 위한 서비스
 */
public class AutoConnectService extends Service implements Runnable {

    public static final String TAG = "Auto";
    public int count =0;
    Thread myThread;

    WifiManager wifimanager;

    private int scanCount = 0;
    String text = "";
    String result = "";

    private List<ScanResult> mScanResult; // ScanResult List
    private ArrayList<String> mScanResultString = new ArrayList<String>();


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                getWIFIScanResult(); // get WIFISCanResult
                wifimanager.startScan(); // for refresh
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
            }
        }
    };

    class ScanResultComparator implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
            int level_of_o1 = (((ScanResult)o1).level);
            int level_of_o2 = (((ScanResult)o2).level);
            return level_of_o2 - level_of_o1;
        }
    }

    public void getWIFIScanResult() {

        mScanResult = wifimanager.getScanResults(); // ScanResult

        Collections.sort(mScanResult, new ScanResultComparator());

        // Scan count
        int scansize = mScanResult.size();
        for (ScanResult result : mScanResult) {
            if (result.level < -80) continue;

            String password = "an453f4956";
            connectWifi(result.SSID, password, result.capabilities);
        }
    }

    /*private boolean connectWifi(ScanResult result) {
        // TODO result의 password 알아내는 방법 구하기
        // 만약 wifi가 비밀번호가 필요한데 password를 모르면 return false
        // 만약 wifi 비밀번호가 필요하지 않으면 아래 return 실행
        return connectWifi(result.SSID, "tmf915419", result.capabilities.toString());
    }*/


    public void initWIFIScan() {
        // init WIFISCAN
        scanCount = 0;
        text = "";
        final IntentFilter filter = new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        wifimanager.startScan();
        Log.d(TAG, "initWIFIScan()");
    }

    public void onCreate(){
        super.onCreate();

        Log.i(TAG,"Autostart");
        // 쓰레드 객체 생성 후 시작
        myThread = new Thread(this);
        myThread.start();

        wifimanager = (WifiManager) getSystemService(WIFI_SERVICE);
        Log.d(TAG, "Setup WIfiManager getSystemService");

        // if WIFIEnabled
        if (wifimanager.isWifiEnabled() == false)
            wifimanager.setWifiEnabled(true);
    }

    public void printToast(String messageToast) {
        Toast.makeText(this, messageToast, Toast.LENGTH_LONG).show();
    }

    public void onDestroy(){
        myThread.interrupt();
    }

    public boolean connectWifi(String ssid, String password, String capablities) {
        WifiConfiguration wfc = new WifiConfiguration();

        wfc.SSID = "\"".concat( ssid ).concat("\"");
        wfc.status = WifiConfiguration.Status.DISABLED;
        wfc.priority = 40;

        if(capablities.contains("WEP") == true ){
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wfc.wepKeys[0] = "\"".concat(password).concat("\"");
            wfc.wepTxKeyIndex = 0;
        }else if(capablities.contains("WPA") == true ) {
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wfc.preSharedKey = "\"".concat(password).concat("\"");
        }else if(capablities.contains("WPA2") == true ) {
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wfc.preSharedKey = "\"".concat(password).concat("\"");
        }else if(capablities.contains("OPEN") == true ) {
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wfc.allowedAuthAlgorithms.clear();
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        }

        int networkId = wifimanager.addNetwork(wfc);

        boolean connection = false;
        if (networkId != -1) {
            printToast("WIFI TRY");
            connection = wifimanager.enableNetwork(networkId, true);
        }
        return connection;
    }


    //run 함수안에서 while 로 스캔 - 정렬 - open / DB검사 -  sleep2초 주기로 반복
    public void run(){
        Log.i(TAG,"Autorun()");
        // 실행부 ( 예시 로그에 5초마다 카운트 )
        // 자동연결 하는 코드
        while(true){
            try{
                // TODO
                // 자동연결 서비스 구현


                Log.i(TAG, "자동연결 서비스 #" + count);
                count++;

                Thread.sleep(5000);
            }catch(InterruptedException ex){
                Log.e(TAG, ex.toString());
                break;
            }
        }
    }
    public IBinder onBind(Intent arg0){
        return null;
    }


}
