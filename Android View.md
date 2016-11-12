# Android View

> 学习Android View框架，了解View的测量、布局、绘制过程，掌握常用的线性布局、相对布局的实现原理和用法，掌握ViewStub、include等高级用法，掌握ListView/RecyclerView的优化方法。

##  View的测量、布局、绘制过程 

0. 开始于ViewRootImpl 

   * performTraversals()

     ```ache mView since it is used so much below…```

     根据之前设置的状态 判断是否measure layout draw

   * getRootMeasureSpec()

     ``` Figures out the measure spec for the root view in a window based on it's layout params.```

1. measure操作 用于确定视图宽度和长度

   ```This is called to find out how big a view should be. The parent supplies constraint information in the width and height parameters.```

   * 根据父视图和自身决定宽和高


   * onMeasure()，视图大小的将在这里最终确定 通过setMeasuredDimension(width, height)保存计算结果

     mMeasuredWidth和mMeasuredHeight赋值 view测量结束

   * onMesure() - getDefaultSize()

      ```Utility to return a default size. Uses the supplied size if the   MeasureSpec imposed no constraints.Will get larger if allowed  by the MeasureSpec.```

   * MeasureSpec（View的内部类）测量规格为int型，值由高2位规格模式specMode和低30位具体尺寸specSize组成

     * MeasureSpec.EXACTLY //确定模式，父View希望子View的大小是确定的，由specSize决定；
     * MeasureSpec.AT_MOST //最多模式，父View希望子View的大小最多是specSize指定的值；
     * MeasureSpec.UNSPECIFIED //未指定模式，父View完全依据子View的设计值来决定； 

   * getSuggestedMinimumWidth/getSuggestedMinimumHeight

     最小宽度和高度由View的Background尺寸与minX属性共同设置

   * ViewGroup在搞事情

     * ViewGroup中存在嵌套现象 使得measure递归传递 measureChild measureChildWithMargins(padding和margin)

     * measureChildWithMargins

       ```Ask one of the children of this view to measure itself, taking into account both the MeasureSpec requirements for this view and its padding and margins. ``` 

       对父视图提供的measureSpec参数结合自身的LayoutParams参数进行调整


   * other

     * MeasureSpec

       ```A MeasureSpec encapsulates the layout requirements passed from parent to child.```

     * measure为final 只能通过重载onMeasure自定义测量逻辑

     * DecorView的MeasureSPec由ViewRootImpl中getRootMeasureSpec方法确定

     * measureChild和measureChildWithMargins简化了父子VIew的尺寸计算

     * ViewGroup的子类就必须要求LayoutParams继承子MarginLayoutParams，否则无法使用layout_margin参数(?)

     * View的getMeasuredWidth()和getMeasuredHeight()方法来获取View测量的宽高，要必须保证这两个方法在onMeasure流程之后被调用才能返回有效值。

2. layout操作 用于确定视图在屏幕中显示的位置

   ```Assign a size and position to a view and all of its descendants This is the second phase of the layout mechanism.```

   0. mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());


   1. onLayout()

      空方法  	view类中可以在子类重写  viewgroup类中与子类中重写

      得到View位置分配后的mLeft mRight mBottom mTop

   ​

3. draw操作 将视图显示到屏幕中

   ``` Manually render this view (and all of its children) to the given Canvas.```


   1. 绘制背景()


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

     ```Inflate a new view hierarchy from the specified xml resource```

3.   LayoutInflater.inflate() - rInflate()

     ```Recursive method used to descend down the xml hierarchy and instantiate views, instantiate their children```

     parent的所有子节点都inflate完毕的时候回onFinishInflate方法 onFinishInflate()为空方法  可以添加自定义逻辑

     ​

     ​