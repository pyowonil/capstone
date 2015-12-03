package com.capstone.theold4.visualwifi;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by park on 2015-12-01.
 */
public class DataAdapter extends ArrayAdapter<CVData> {
    // 레이아웃 XML을 읽어들이기 위한 객체
    private LayoutInflater mInflater;
    public DataAdapter(Context context, ArrayList<CVData> object) {
        // 상위 클래스의 초기화 과정
        // context, 0, 자료구조
        super(context, 0, object);
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    // 자신이 만든 xml의 스타일로 보이기 위한 구문
    @Override
    public View getView(int position, View v, ViewGroup parent) {
        View view = null;

        // 현재 리스트의 하나의 항목에 보일 컨트롤 얻기
        if (v == null) {
            // XML 레이아웃을 직접 읽어서 리스트뷰에 넣음
            view = mInflater.inflate(R.layout.list_row, null);
        } else {
            view = v;
        }
        // 자료를 받는다.
        final CVData data = this.getItem(position);
        if (data != null) {
            // 화면 출력
            TextView tv = (TextView) view.findViewById(R.id.mText);
            TextView tv2 = (TextView) view.findViewById(R.id.mText2);
            // 텍스트뷰1에 getLabel()을 출력 즉 첫번째 인수값
            tv.setText(data.getLabel());
            // 텍스트뷰2에 getData()을 출력 즉 두번째 인수값
            tv2.setText(data.getData());
            tv2.setTextColor(Color.rgb(180,80,120));

            ImageView imageview = (ImageView)view.findViewById(R.id.mImage);

            // 이미지뷰에 뿌려질 해당 이미지값을 연결 즉 세번째 인수값
            imageview.setImageResource(data.getData2());
        }
        return view;
    }
}