package com.capstone.theold4.visualwifi;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class Guide extends AppCompatActivity implements OnClickListener{
    Button btn_finish;
    private float lastX;
    private ViewFlipper viewFlipper;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        btn_finish = (Button) findViewById(R.id.btn_finish);

        btn_finish.setOnClickListener(this);
        viewFlipper = (ViewFlipper) findViewById(R.id.viewflipper);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btn_finish) {
            Log.i("btn", "OnClick() btn_finish()");
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    // Using the following method, we will handle all screen swaps.
    public boolean onTouchEvent(MotionEvent touchevent) {
        switch (touchevent.getAction()) {

            case MotionEvent.ACTION_DOWN:
                lastX = touchevent.getX();
                break;
            case MotionEvent.ACTION_UP:
                float currentX = touchevent.getX();

                // Handling left to right screen swap.
                if (lastX < currentX) {

                    // If there aren't any other children, just break.
                    if (viewFlipper.getDisplayedChild() == 0)
                        break;

                    // Next screen comes in from left.
                    viewFlipper.setInAnimation(this, R.anim.slide_in_from_left);
                    // Current screen goes out from right.
                    viewFlipper.setOutAnimation(this, R.anim.slide_out_to_right);

                    // Display next screen.
                    viewFlipper.showNext();
                }

                // Handling right to left screen swap.
                if (lastX > currentX) {

                    // If there is a child (to the left), kust break.
                    if (viewFlipper.getDisplayedChild() == 1)
                        break;

                    // Next screen comes in from right.
                    viewFlipper.setInAnimation(this, R.anim.slide_in_from_right);
                    // Current screen goes out from left.
                    viewFlipper.setOutAnimation(this, R.anim.slide_out_to_left);

                    // Display previous screen.
                    viewFlipper.showPrevious();
                }
                break;
        }
        return false;
    }

}
