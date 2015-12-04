package com.example.cute.manual;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnClickListener {

    private WifiManager mWifiManager;
    private String string;
    private List<ScanResult> mScanResult;
    private ArrayList<String> mScanResultString = new ArrayList<String>();

    // 리스트에서 선택한 내용으로 Connect에 사용
    private String mSelectedSSID, mSelectedCapability;

    Button btn_alert;
    EditText edt_ssid;
    EditText edt_pwd;
    TextView txtv_capa;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                // TODO getWIFIScanResult(); // get WIFISCanResult

                mWifiManager.startScan(); // for refresh
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_alert = (Button) findViewById(R.id.btn_alert);
        btn_alert.setOnClickListener(this);
        edt_ssid = (EditText) findViewById(R.id.edt_ssid);
        edt_pwd = (EditText) findViewById(R.id.edt_pwd);
        txtv_capa = (TextView) findViewById(R.id.txtv_capa);

        mWifiManager = (WifiManager)getSystemService(WIFI_SERVICE);

        // Wifi Init
        final IntentFilter filter = new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        mWifiManager.startScan();
    }

    class ScanResultComparator implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
            int level_of_o1 = (((ScanResult)o1).level);
            int level_of_o2 = (((ScanResult)o2).level);
            return level_of_o2 - level_of_o1;
        }
    }

    public void getWIFIScanResult() {

        mScanResult = mWifiManager.getScanResults(); // ScanResult

        // Scan count
        mScanResultString.clear();
        Collections.sort(mScanResult, new ScanResultComparator());

        int count=1;
        for(ScanResult result : mScanResult) {
            if (result.level < -80) continue;
            mScanResultString.add(count + ". SSID(" + result.SSID.toString() + ") RSSI(" + result.level + "dBm) MAC(" + result.BSSID + ") Capa(" + result.capabilities + ")");
            // ArrayList 값 확인
            count += 1;
        }
    }

    public void initWIFIScan() {
        // init WIFISCAN
        final IntentFilter filter = new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        mWifiManager.startScan();
        Log.i("scan", "initWIFIScan()");
    }

    public void list() {
        getWIFIScanResult();
        final String[] scanResults = new String[mScanResultString.size()];
        mScanResultString.toArray(scanResults);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("ScanResultList");
        builder.setItems(scanResults, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // will toast your selection
                mSelectedSSID = mScanResult.get(item).SSID;
                mSelectedCapability = mScanResult.get(item).capabilities;
                dialog.dismiss();

                alertlogin();
            }
        });
        builder.show();
    }

    public void alertlogin(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View innerView = getLayoutInflater().inflate(R.layout.login, null);
        builder.setView(innerView);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                edt_ssid = (EditText)innerView.findViewById(R.id.edt_ssid);
                String ssid = edt_ssid.getText().toString();
                edt_pwd = (EditText)innerView.findViewById(R.id.edt_pwd);
                String pwd = edt_pwd.getText().toString();

                Log.i("tttt", ssid + " " + pwd + " " + mSelectedSSID + " " + mSelectedCapability);

                connectWifi(mSelectedSSID, pwd, mSelectedCapability);
            }
        });
        builder.show();

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_alert) {
            Log.i("cheche", "OnClick() btn_alert");
            initWIFIScan();
            list();
        }
    }
    public boolean connectWifi(String ssid, String password, String capablities) {
        boolean success = false;
        //  TODO 성공하면 success -> true
        WifiConfiguration wfc = new WifiConfiguration();

        wfc.SSID = "\"".concat( ssid ).concat("\"");
        wfc.status = WifiConfiguration.Status.ENABLED;
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
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
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
        }else if(capablities.contains("OPEN") == true || capablities.contains("ESS") == true) {
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

        if (networkId != -1) {
            string += ("WIFI TRY ... ");
            success = mWifiManager.enableNetwork(networkId, true);
            if(success) {
                string += ("SUCCESS");
            } else {
                string += ("FAIL");
            }
        }

        return success;
    }
}
