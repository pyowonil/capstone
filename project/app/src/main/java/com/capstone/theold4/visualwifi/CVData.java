package com.capstone.theold4.visualwifi;

import android.content.Context;

/**
 * Created by park on 2015-12-01.
 */
public class CVData {
    private String m_szLabel;
    private String m_szData;
    private int m_szData2;
    public CVData(Context context, String p_szLabel, String p_szDataFile,
                  int p_szData2) {
        m_szLabel = p_szLabel;
        m_szData = p_szDataFile;
        m_szData2 = p_szData2;
    }
    public String getLabel() {
        return m_szLabel;
    }
    public String getData() {
        return m_szData;
    }
    public int getData2() {
        return m_szData2;
    }
}
