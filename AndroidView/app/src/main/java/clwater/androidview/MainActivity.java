package clwater.androidview;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import clwater.androidview.view.TextButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

    private LinearLayout linearLayout;
    private TextButton testbtn;
    private Button btn;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
//
//        linearLayout = (LinearLayout) findViewById(R.id.mylayout);
//        testbtn = (TextButton) findViewById(R.id.test_mybtn);
//
//        linearLayout.setOnTouchListener(this);
//        testbtn.setOnTouchListener(this);
//
//        linearLayout.setOnClickListener(this);
//        testbtn.setOnClickListener(this);

        activity = this;

        btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.setContentView(R.layout.activity_main3);
            }
        });
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i("view", "OnTouchListener--onTouch-- action="+event.getAction()+" --"+v);
        return false;
    }

    @Override
    public void onClick(View v) {
        Log.i("view", "OnClickListener--onClick--"+v);
    }
}
