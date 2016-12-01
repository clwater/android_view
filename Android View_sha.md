## RelativeLayout  源码分析
> 继承自ViewGroup 没有重载onDraw方法 内部子View又是相对 只要计算出View的坐标 layout过程同样简单
 
    
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //  The layout has actually already been performed and the positions
        //  cached.  Apply the cached values to the children.
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                RelativeLayout.LayoutParams st =
                        (RelativeLayout.LayoutParams) child.getLayoutParams();
                child.layout(st.mLeft, st.mTop, st.mRight, st.mBottom);
            }
        }
    }
    
    
### RealtiveLayout的measure过程
#### 主要过程
1. 将内部View根据纵向关系和横向关系排序
2. 初始化相关变量
3. 遍历水平关系的View
4. 遍历竖直关系的View
5. baseline计算
6. 宽度和高度修正

#### 1 将内部View根据纵向关系和横向关系排序
>layout_toRightOf 为横向关系  layout_below为纵向关系
 
    //首先会根据mDirtyHierarchy的值判断是否需要将子View重新排序
    if (mDirtyHierarchy) {
        mDirtyHierarchy = false;
        sortChildren();
    }
    //mDirtyHierarchy的值只有在requestLayout方法下被更新 
    public void requestLayout() {
        super.requestLayout();
        mDirtyHierarchy = true;
    }
    
    //
