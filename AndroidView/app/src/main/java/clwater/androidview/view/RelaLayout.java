package clwater.androidview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * Created by gengzhibo on 16/12/1.
 */

public class RelaLayout extends RelativeLayout {
    public RelaLayout(Context context) {
        super(context);
    }

    public RelaLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RelaLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RelaLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
