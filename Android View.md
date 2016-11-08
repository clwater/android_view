# Android View

> 学习Android View框架，了解View的测量、布局、绘制过程，掌握常用的线性布局、相对布局的实现原理和用法，掌握ViewStub、include等高级用法，掌握ListView/RecyclerView的优化方法。 


##  View的测量、布局、绘制过程

1. measure操作 用于确定视图宽度和长度

   ```This is called to find out how big a view should be. The parent supplies constraint information in the width and height parameters.```

   1. onMeasure()，视图大小的将在这里最终确定 通过setMeasuredDimension(width, height)保存计算结果

   ​

2. layout操作 用于确定视图在屏幕中显示的位置

   ```Assign a size and position to a view and all of its descendant```

   1. setFrame(l,t,r,b)  保存子视图在父视图中的具体位置
   2. onLayout() 为viewGroup类型布局子视图

3. draw操作 将视图显示到屏幕中

   ``` Manually render this view (and all of its children) to the given Canvas.```





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