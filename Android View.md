# Android View

> 学习Android View框架，了解View的测量、布局、绘制过程，掌握常用的线性布局、相对布局的实现原理和用法，掌握ViewStub、include等高级用法，掌握ListView/RecyclerView的优化方法。

##  View的测量、布局、绘制过程  

0. 开始于ViewRootImpl

   * performTraversals()

     ache mView since it is used so much below…

     根据之前设置的状态 判断是否measure layout draw

   * getRootMeasureSpec()

      Figures out the measure spec for the root view in a window based on it's layout params.

1. measure操作 用于确定视图宽度和长度

   This is called to find out how big a view should be. The parent supplies constraint information in the width and height parameters.

   * 根据父视图和自身决定宽和高


*  onMeasure()，视图大小的将在这里最终确定 通过setMeasuredDimension(width, height)保存计算结果

   mMeasuredWidth和mMeasuredHeight赋值 view测量结束

*  onMesure() - getDefaultSize()

          Utility to return a default size. Uses the supplied size if the   MeasureSpec imposed no constraints.Will get larger if allowed  by the MeasureSpec.

   * MeasureSpec（View的内部类）测量规格为int型，值由高2位规格模式specMode和低30位具体尺寸specSize组成

     * MeasureSpec.EXACTLY //确定模式，父View希望子View的大小是确定的，由specSize决定；
     * MeasureSpec.AT_MOST //最多模式，父View希望子View的大小最多是specSize指定的值；
     * MeasureSpec.UNSPECIFIED //未指定模式，父View完全依据子View的设计值来决定；

   * getSuggestedMinimumWidth/getSuggestedMinimumHeight

     最小宽度和高度由View的Background尺寸与minX属性共同设置

   * ViewGroup在搞事情

     * ViewGroup中存在嵌套现象 使得measure递归传递 measureChild measureChildWithMargins(padding和margin)

     * measureChildWithMargins

       Ask one of the children of this view to measure itself, taking into account both the MeasureSpec requirements for this view and its padding and margins.

       对父视图提供的measureSpec参数结合自身的LayoutParams参数进行调整


*    other

     * MeasureSpec

       A MeasureSpec encapsulates the layout requirements passed from parent to child.

     * measure为final 只能通过重载onMeasure自定义测量逻辑

     * DecorView的MeasureSPec由ViewRootImpl中getRootMeasureSpec方法确定

     * measureChild和measureChildWithMargins简化了父子VIew的尺寸计算

     * ViewGroup的子类就必须要求LayoutParams继承子MarginLayoutParams，否则无法使用layout_margin参数(?)

     * View的getMeasuredWidth()和getMeasuredHeight()方法来获取View测量的宽高，要必须保证这两个方法在onMeasure流程之后被调用才能返回有效值。

2. layout操作 用于确定视图在屏幕中显示的位置

   Assign a size and position to a view and all of its descendants This is the second phase of the layout mechanism.

   0. mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());

      相对于parent 的左上右下坐标

   1. layout 判读view的位置是否变更 判断是否需要重新layout

   2. onLayout()

      空方法  	view类中可以在子类重写  viewgroup类中于子类中重写

      得到View位置分配后的mLeft mRight mBottom mTop

   3. other

      顶层父View向子View递归调用view.layout

      得到对每个view进行位置分配后的mLedt , mTop , mRight , mBottom

      layout_XXX等布局属性针对包含子View的ViewGroup

      使用VIew的getWidth和getHeight需要等到onLayout执行之后



3.   draw操作 将视图显示到屏幕中

      Manually render this view (and all of its children) to the given Canvas.

     draw

     java
     	 /*
     * Draw traversal performs several drawing steps which must be executed
        * in the appropriate order:
        *
        *      1. Draw the background
        *      2. If necessary, save the canvas' layers to prepare for fading
        *      3. Draw view's content
        *      4. Draw children
        *      5. If necessary, draw the fading edges and restore layers
        *      6. Draw decorations (scrollbars for instance)
        *		...
        * 		skip step 2 & 5 if possible (common case)
        *		...
        */







1.    Draw the background

        drawBackground(canvas)

      实现了背景的绘制 通过layout中view位置来设置背景的绘制区域 调用Drawable的draw方法来完成背景的绘制工作  



2.    save the canvas' layers

      if (drawTop) {
        canvas.saveLayer(left, top, right, top + length, null, flags);}

        保存layer缺省情况下只有个layer 一般情况不进行这个操作



3.    draw the content

      * onDraw(canvas); 调用onDraw方法绘制

        * onDraw(canvas)

        Implement this to do your drawing

        空方法  需要子类实现

4.    Draw children

      * 对当前View的所有子View进行绘制 如果有

      * dispatchDraw(canvas);

      * dispatchDraw()

        Called by draw to draw the child views

      * View中dispatchDraw方法为空 ViewGroup中实现该方法

      * 遍历ViewGroup中的子VIew通过调用drawChild()方法 调用子View draw()方法

5.    draw the fade effect and restore layers

       绘制阴影效果和回复layer层

6.    draw decorations (scrollbars)

      - 绘制滚动条
      - onDrawForeground(canvas);

7.    other

      - ViewGroup会递归其包含的子View
      - 绘制需要在子类中实现
      - 借助onDraw中传入的canvas类进行
      - 递归顺序和添加顺序一致 可以通过ViewGroup.getChildDrawingOrder()方法重载后修改

      ​

8.    View 的invalidate和postlnvalidate方法()

      ​

## setContentView与LayoutInflater加载解析机制分析

### setContentView分析

1. Window PhoneWindow DecorView Activity关系

   * Window为抽象类 提供绘制窗口通用api

   * PhoneWindow 实现Window

   * DecorView为PhoneWindow内部类 是所有Activity界面的根View

2. Activity - setContentView

   1. PhoneWindow - setContentView

      用于将资源文件通过Layoutinflater转换为View树 ,并添加到mContentParent视图中

   2. PhoneWindow -installDecor

      根据窗口对应的style修饰相依的样式

   3. Activity-onContentChanged

      方法为空 通过attach调用 当activity布局改动时调用 (finViewById()建议放入)

   PhoneWindow类的setContentView方法最后通过调运`mLayoutInflater.inflate(layoutResID, mContentParent);`或者`mContentParent.addView(view, params);`语句将我们的xml或者JavaView插入到了mContentParent（id为content的FrameLayout对象）ViewGroup中。最后setContentView还会调用一个Callback接口的成员函数onContentChanged来通知对应的Activity组件视图内容发生了变化。

   4. 整体流程
   5. 创建DecorView对象mView , 该mView对象作为整个应用窗口的根视图
      2. 根据Feature等style theme创建不同的窗口修饰布局  通过findViewById获取Activity布局文件该存放				    	的地方(id为content的FrameLayout)
   6. 将Activity的布局文件添加至id为content的FrameLayout内

      ​***到这里 Activity还是没有显示出来页面***

3. setContentView完成后Activity显示界面

   Activity调运完ActivityThread的main(实际上Activity的开始)方法后 , 调用ActivityThread类的performLaunchActivity来创建要启动的Activity组件 ,这个过程中为该Activity组件创建窗口对象和视图对象 ,调运ActivityThread类的handleResumeActivity将它激活

   ​	handleResumeActivity中 r.activity.makeVisible()  -> Activity - mDector.setVisibility(View.VISIBLE)



### LayoutInflater 分析

1.   LayoutInflater的实例化  

      封装 ContextThemeWrapper - getSystemService() 来获取实例化

2.   LayoutInflater.inflate()们

      Inflate a new view hierarchy from the specified xml resource

3.   LayoutInflater.inflate() - rInflate()

    Recursive method used to descend down the xml hierarchy and instantiate views, instantiate their children

     parent的所有子节点都inflate完毕的时候回onFinishInflate方法 onFinishInflate()为空方法  可以添加自定义逻辑


## LinearLayout


     > View的measure方法为final 只能通过重载onMeasure实现组件自己的测量逻辑

     1. onMeasure

     java
     protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {  
     	if (mOrientation == VERTICAL) {
             measureVertical(widthMeasureSpec, heightMeasureSpec);
         } else {
             measureHorizontal(widthMeasureSpec, heightMeasureSpec);
         }
     }

     判断布局方向

     2. measureVertical (measureVertical 与measureHorizontal实现类似)

        Measures the children when the orientation of this LinearLayout is set to {@link #VERTICAL}.

        *    相关变量

             java
                //记录内部使用的高度 不是LinearLayout的高度 =-=
                 mTotalLength = 0;
             	//所有子View中 宽度最大的值
                 int maxWidth = 0;
             	//子View的测量状态 (通过combineMeasuredStates 按位相或 合并)
                 int childState = 0;

             	//当matchWidthLocally参数为真时 跟当前子控件的左右margin和相比较取大值
             	//weight<=0的View最大值
                 int alternativeMaxWidth = 0;
             	//weight>0的View 的最大值
                 int weightedMaxWidth = 0;
             	//是否子View均为match_Parent 判断是否需要重新测量
                 boolean allFillParent = true;
                 //weight的和
                 float totalWeight = 0;

                 //子View的数量 下一级的数量 而不是所有子View的数量
                 final int count = getVirtualChildCount();

             	//高度宽度模式
                 final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
                 final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

             	//判断子View是否为match_parent matchWidthLocally 觉得了子View 的测量时父View干预		//还是填充父View
                 boolean matchWidth = false;
                 boolean skippedMeasure = false;

                 final int baselineChildIndex = mBaselineAlignedChildIndex;        
                 final boolean useLargestChild = mUseLargestChild;

                 int largestChildHeight = Integer.MIN_VALUE;


        *    测量

             java
                    for (int i = 0; i < count; ++i) {
                         final View child = getVirtualChildAt(i);

                         if (child == null) {
                           	//measureNullChild() 任何情况下均返回0
                             mTotalLength += measureNullChild(i);
                             continue;
                         }
             			//Visibility为Gone的时候跳过该View
                      	//getChildrenSkipCount() 方法同样任何情况下均返回0
                         if (child.getVisibility() == View.GONE) {
                            i += getChildrenSkipCount(child, i);
                            continue;
                         }

                         //根据showDivider的值(通过hasDividerBeforeChildAt()) 来决定当前子View
                      	//是否需要添加drvider的高度
                         if (hasDividerBeforeChildAt(i)) {
                             mTotalLength += mDividerHeight;
                         }
                         //父容器在add,measure时会将子View的LayoutParams 强转为自己的类型
                         LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();

                         //权重
                         totalWeight += lp.weight;

             			//weight 相关分支

                         //当LinearLayout是EXACLY模式或有具体的值) 子View的高度为0 weight大于0
                         //在LinearLayout高度有剩余的时候会根据权重分配高度(二次测量)
                         if (heightMode == MeasureSpec.EXACTLY && lp.height == 0 && lp.weight > 0) {
                             final int totalLength = mTotalLength;
                             mTotalLength = Math.max(totalLength, totalLength + lp.topMargin + lp.bottomMargin);
                             skippedMeasure = true;
                         } else {

                             //其他情况
                             int oldHeight = Integer.MIN_VALUE;

                             if (lp.height == 0 && lp.weight > 0) {
             					//相当于父类为为wrap_layout 或者为UNSPECIFIED模式
                                 //将子VIew的高度设置为-1(WRAP_CONTCNT)
             					//防止子空间高度为0
                                 oldHeight = 0;
                                 lp.height = LayoutParams.WRAP_CONTENT;
                             }

                             //开始测量子VIew
                             //当LinearLayout不是EXACLY模式 且子VIew的weight大于0 会优先把LIinearLayout的全部可用高度用于子View的测量
                           	//实际上调用ViewGroup中的getChildMeasureSpec()
                           	//当之前的weight为0 进行正常测量 否则LinearLayout传入0
                           	//结合父View的MeasureSpec子View的LayoutParams 对子View进行测量
                           	//在测量View之前 如果weight为0 需要考虑之前分配的高度 根据剩余
                             measureChildBeforeLayout(
                                    child, i, widthMeasureSpec, 0, heightMeasureSpec,
                                    totalWeight == 0 ? mTotalLength : 0);
                            //重置子VIew高度
                                   if (oldHeight != Integer.MIN_VALUE) {
                                      lp.height = oldHeight;
                                   }

                                   final int childHeight = child.getMeasuredHeight();
                                   final int totalLength = mTotalLength;

                                   //getNextLocationOffset()返回0
                                  //比较child测量前后总高度 取较大值
                                   mTotalLength = Math.max(totalLength, totalLength + childHeight + lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));

                           if (useLargestChild) {
                                       largestChildHeight = Math.max(childHeight, largestChildHeight);
                                   }
                               }
                      if ((baselineChildIndex >= 0) && (baselineChildIndex == i + 1)) {
                                  mBaselineChildTop = mTotalLength;
                               }

                               if (i < baselineChildIndex && lp.weight > 0) {
                                   throw new RuntimeException("A child of LinearLayout with index " + "less than mBaselineAlignedChildIndex has weight > 0, which "
             + "won't work.  Either remove the weight, or don't set "+ "mBaselineAlignedChildIndex.");
                       }

                       boolean matchWidthLocally = false;
                    	//当父View非EXACTLY或精确值 子View为match_parent
                    	//matchWidthLocally和matchWidth置为true
                    	//该View或占据父View水平方向所有空间
                       if (widthMode != MeasureSpec.EXACTLY && lp.width == LayoutParams.MATCH_PARENT) {
                           matchWidth = true;
                           matchWidthLocally = true;
                       }

                       final int margin = lp.leftMargin + lp.rightMargin;
                       final int measuredWidth = child.getMeasuredWidth() + margin;
                       maxWidth = Math.max(maxWidth, measuredWidth);
                       childState = combineMeasuredStates(childState, child.getMeasuredState());

                       allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT;
                       if (lp.weight > 0) {
                           weightedMaxWidth = Math.max(weightedMaxWidth,
                                   matchWidthLocally ? margin : measuredWidth);
                       } else {
                           alternativeMaxWidth = Math.max(alternativeMaxWidth,
                                   matchWidthLocally ? margin : measuredWidth);
                       }

                       i += getChildrenSkipCount(child, i);
                    }


        *    测量相关

             - 根据LinearLayout模式分成两部分 EXACTLY模式下且weight不为0 且高度为0的子View优先级低 如果LinearLayout剩余空间不足则不显示 但是如果是AT_MOST的weight不为0 企鹅高度设置为0会优先获得高度
             - 为LinearLayout动态添加子View的时候，子View的LayoutParams一定要是LinearLayout的内部类(ViewGroup通用)

        *    weight - weight的再次测量

             java
                     //useLargestChild属性相关
                     if (mTotalLength > 0 && hasDividerBeforeChildAt(count)) {
                         mTotalLength += mDividerHeight;
                     }
                     if (useLargestChild &&
                             (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED)) {
                         mTotalLength = 0;
                         for (int i = 0; i < count; ++i) {
                             final View child = getVirtualChildAt(i);
                             if (child == null) {
                                 mTotalLength += measureNullChild(i);
                                 continue;
                             }
                             if (child.getVisibility() == GONE) {
                                 i += getChildrenSkipCount(child, i);
                                 continue;
                             }
                             final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)
                                     child.getLayoutParams();
                             // Account for negative margins
                             final int totalLength = mTotalLength;
                             mTotalLength = Math.max(totalLength, totalLength + largestChildHeight +
                                     lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));
                         }
                     }
                     // Add in our padding
                     mTotalLength += mPaddingTop + mPaddingBottom;
                     int heightSize = mTotalLength;
                     // Check against our minimum height
                     heightSize = Math.max(heightSize, getSuggestedMinimumHeight());
                     // Reconcile our calculated size with the heightMeasureSpec
                     int heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0);
                     heightSize = heightSizeAndState & MEASURED_SIZE_MASK;
