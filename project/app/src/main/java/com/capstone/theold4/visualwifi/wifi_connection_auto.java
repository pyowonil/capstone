package com.capstone.theold4.visualwifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class wifi_connection_auto extends Service {
    private WifiManager mWifiManager;
    private List<ScanResult> mScanResult;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                // TODO 와이파이 스캔 결과 얻기
                mWifiManager.startScan();
            } else if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                // TODO 와이파이 ... 이거 필요한가?
            }
        }
    };

    // 현재 연결된 상태 표시
    private boolean mIsConnection = true;
    private ConnectivityManager mConnectivityManager;

    // 정렬 알고리즘
    private class ScanResultComparator implements Comparator<ScanResult> {
        @Override
        public int compare(ScanResult lhs, ScanResult rhs) {
            return rhs.level - lhs.level;
        }
    }
    private Comparator<ScanResult> mScanResultComparator = new ScanResultComparator();

    // = = = = = = = = = = 서비스 쓰레드 = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    private Thread mThread;
    private Runnable mRun = new Runnable() {
        @Override
        public void run() {
            try{
                while(!Thread.currentThread().isInterrupted()) {
                    // 와이파이 자동 연결 시도
                    if(!mIsConnection) {
                        mScanResult = mWifiManager.getScanResults();
                        Collections.sort(mScanResult, mScanResultComparator);
                        for (ScanResult scan : mScanResult) {
                            String SSID = scan.SSID;
                            // TODO 패스워드 매커니즘 필요
                            String password = "4567253885844163";
                            String capability = scan.capabilities;
                            Log.i("[WIFI]", SSID + " " + capability);
                            if (connect(SSID, password, capability)) {
                                Thread.sleep(1500);
                                NetworkInfo.State wifistate = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
                                if(wifistate == NetworkInfo.State.DISCONNECTED || wifistate == NetworkInfo.State.DISCONNECTING) {
                                    Log.i("[WIFI]", "Disconnection");
                                    continue;
                                }
                                if(wifistate == NetworkInfo.State.CONNECTING || wifistate == NetworkInfo.State.CONNECTED) {
                                    Log.i("[WIFI]", "Connect");
                                    mIsConnection = true;
                                    Thread.sleep(10000);
                                    break;
                                }
                            }
                        }
                    } else {
                        // 현재 연결이 끊기는 경우 잡아서 반영함
                        NetworkInfo.State wifistate = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
                        if(wifistate == NetworkInfo.State.CONNECTED) {
                            Log.i("[WIFI]", "Continue");
                            Thread.sleep(10000);
                        } else {
                            mIsConnection = false;
                        }
                    }

                    Thread.sleep(100);
                }
            }catch(Exception e) {
                e.printStackTrace();
            }finally {
                // TODO 종료전 마지막 작업
            }
        }
    };
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 서비스 쓰레드 = = = = = = = = = =

    // = = = = = = = = = = 액티비티 시작 (onCreate) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    @Override
    public void onCreate() {
        super.onCreate();
        // 와이파이 관련 변수들 초기화
        mWifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        mWifiManager.startScan();
        mConnectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 액티비티 시작 (onCreate) = = = = = = = = = =

    // = = = = = = = = = = 서비스 시작 (onStartCommand) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        mThread.interrupt();
        try {
            mThread.join(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 서비스 종료 (onDestroy) = = = = = = = = = =

    // = = = = = = = = = = 서비스 바인드 (onBind) = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 서비스 바인드 (onBind) = = = = = = = = = =

    // = = = = = = = = = = 와이파이 연결 = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    public boolean connect(String SSID, String password, String capability) {
        // TODO 자세한 메커니즘 확인 필요
        boolean success = false;
        WifiConfiguration wfc = new WifiConfiguration();

        wfc.SSID = "\"".concat( SSID ).concat("\"");
        wfc.status = WifiConfiguration.Status.ENABLED;
        wfc.priority = 40;

        if(capability.contains("WEP") == true ){
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
        }else if(capability.contains("WPA") == true ) {
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wfc.preSharedKey = "\"".concat(password).concat("\"");
        }else if(capability.contains("WPA2") == true ) {
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wfc.preSharedKey = "\"".concat(password).concat("\"");
        }else if(capability.contains("OPEN") == true ) {
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

        int networkId = mWifiManager.addNetwork(wfc);
        if(networkId != -1) {
            success = mWifiManager.enableNetwork(networkId, true);
        }

        return success;
    }
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 와이파이 연결 = = = = = = = = = =
}
