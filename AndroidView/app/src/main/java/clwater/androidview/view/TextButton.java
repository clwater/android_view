package clwater.androidview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;

/**
 * Created by gengzhibo on 16/11/2.
 */

public class TextButton extends Button {
    public TextButton(Context context , AttributeSet attre) {
        super(context , attre);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        String a = "a";
        Log.i("view", "dispatchTouchEvent-- action=" + event.getAction());
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i("view", "onTouchEvent-- action="+event.getAction());
        return false;

    }
}
