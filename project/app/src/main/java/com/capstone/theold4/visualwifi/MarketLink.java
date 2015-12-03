package com.capstone.theold4.visualwifi;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by park on 2015-12-01.
 */
public class MarketLink extends Activity {

    ImageView imageView;
    TextView textView;
    int deviceNum = 0;
    Uri uri = null;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.market);

        Intent intent = getIntent();
        deviceNum = intent.getIntExtra("DeviceNum", deviceNum);
        imageView = (ImageView)findViewById(R.id.imageView);
        textView = (TextView)findViewById(R.id.textView);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                startActivity(intent);
            }
        };

        switch (deviceNum){
            case 0:
                imageView.setImageResource(R.drawable.device01);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=3567820&cate=112804");
                textView.setText("EFM ipTIME A3004-dual 유무선공유기" +
                        "\n 66,000원 가격 대비 성능 좋음");
                break;
            case 1:
                imageView.setImageResource(R.drawable.device02);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=2463749&cate=112804");
                textView.setText("TP-LINK Archer C7 AC1750 유무선공유기" +
                        "\n 76,240원 가격 대비 성능 좋음");
                break;
            case 2:
                imageView.setImageResource(R.drawable.device03);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=2754623&cate=112804");
                textView.setText("ASUS RT-AC68U 유무선공유기" +
                        "\n 257,610원 가격은 비싸지만 성능은 최고 막힘 없음");
                break;
            case 3:
                imageView.setImageResource(R.drawable.device04);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=1957655&cate=112804");
                textView.setText("D-Link DIR-850L 유무선공유기" +
                        "\n 54,250 가격 대비 성능 좋음");
                break;
            case 4:
                imageView.setImageResource(R.drawable.device05);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=3275636&cate=112804");
                textView.setText("TP-LINK Archer C2 AC750 유무선공유기" +
                        "\n 34,900원 가격 저렴 성능 좋음");
                break;
        }

    }

    public void onDestroy(){
        super.onDestroy();
        Intent intent = new Intent(MarketLink.this, visual_wifi_map.class);
        startActivity(intent);
        finish();
    }
}
