package clwater.androidview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import clwater.androidview.view.TextButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

    private LinearLayout linearLayout;
    private TextButton testbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//
//        linearLayout = (LinearLayout) findViewById(R.id.mylayout);
//        testbtn = (TextButton) findViewById(R.id.test_mybtn);
//
//        linearLayout.setOnTouchListener(this);
//        testbtn.setOnTouchListener(this);
//
//        linearLayout.setOnClickListener(this);
//        testbtn.setOnClickListener(this);
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
