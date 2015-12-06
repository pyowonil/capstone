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
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=2472103&keyword=%B0%F8%C0%AF%B1%E2");
                textView.setText(R.string.device01_detail + "\n" + R.string.device01_info);
                break;
            case 1:
                imageView.setImageResource(R.drawable.device02);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=2463749&cate=112804");
                textView.setText(R.string.device02_detail +"\n" + R.string.device02_info);
                break;
            case 2:
                imageView.setImageResource(R.drawable.device03);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=2754623&cate=112804");
                textView.setText(R.string.device03_detail +"\n" + R.string.device03_info);
                break;
            case 3:
                imageView.setImageResource(R.drawable.device04);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=1957655&cate=112804");
                textView.setText(R.string.device04_detail +"\n" + R.string.device04_info);
                break;
            case 4:
                imageView.setImageResource(R.drawable.device05);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=3275636&cate=112804");
                textView.setText(R.string.device05_detail +"\n" + R.string.device05_info);
                break;
            case 5:
                imageView.setImageResource(R.drawable.device03);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=2754597&keyword=%B0%F8%C0%AF%B1%E2");
                textView.setText(R.string.device03_detail +"\n" + R.string.device03_info);
                break;
            case 6:
                imageView.setImageResource(R.drawable.device04);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=2214352&keyword=%B0%F8%C0%AF%B1%E2");
                textView.setText(R.string.device04_detail +"\n" + R.string.device04_info);
                break;
            case 7:
                imageView.setImageResource(R.drawable.device05);
                imageView.setOnClickListener(onClickListener);
                uri = Uri.parse("http://prod.danawa.com/info/?pcode=1533180&keyword=%B0%F8%C0%AF%B1%E2");
                textView.setText(R.string.device05_detail +"\n" + R.string.device05_info);
                break;

        }

    }

    public void onDestroy(){
        super.onDestroy();
        finish();
    }
}
