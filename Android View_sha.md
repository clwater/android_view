# Android View
>Android View框架相关  View的测量 布局 绘制过程
LinearLayout RelativeLayout实现源码分析

## 从setContentView与LayoutInflater加载解析机制说起
### setContentView分析
#### 相关关系

  ![相关关系图](/image_1.png)  

  Activity中有Window成员 实例化为PhoneWindow PhoneWindow是抽象Window类的实现类

  Window提供了绘制窗口的通用API PhoneWindow中包含了DecorView对象 是所有窗口(Activity界面)的根View

  具体的构如下

  ![View层级分析](/View层级分析.png)

  通过hierarchyviewer工具分析一下



#### PhoneWindow的setContentView分析
>Window类的setContentView方法 而Window的setContentView方法是抽象的  所以查看PhoneWindow的setContentView()

1. setContentView方法
  ```java
    // This is the view in which the window contents are placed. It is either
    // mDecor itself, or a child of mDecor where the contents go.
    private ViewGroup mContentParent;

    @Override
    public void setContentView(int layoutResID) {
        // Note: FEATURE_CONTENT_TRANSITIONS may be set in the process of installing the window
        // decor, when theme attributes and the like are crystalized. Do not check the feature
        // before this happens.
        if (mContentParent == null) {
            //第一次调用
            //下面会详细分析
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            //移除该mContentParent下的所有View
            //又因为这个的存在  我们可以多次使用setContentView()
            mContentParent.removeAllViews();
        }
        //判断是否使用了Activity的过度动画
        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
          //设置动画场景
            final Scene newScene = Scene.getSceneForLayout(mContentParent, layoutResID,
                    getContext());
            transitionTo(newScene);
        } else {
            //将资源文件通过LayoutInflater对象装换为View树
            //在PhoneWindow的构造函数中 mLayoutInflater = LayoutInflater.from(context);
            mLayoutInflater.inflate(layoutResID, mContentParent);
        }

        //View中
        /**
         * Ask that a new dispatch of {@link #onApplyWindowInsets(WindowInsets)} be performed.
         */
        // public void requestApplyInsets() {
        //     requestFitSystemWindows();
        // }
        mContentParent.requestApplyInsets();
        final Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
    }

    @Override
    public void setContentView(View view) {
        setContentView(view, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (mContentParent == null) {
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }

        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            view.setLayoutParams(params);
            final Scene newScene = new Scene(mContentParent, view);
            transitionTo(newScene);
        } else {
          //已经为View 直接使用View的addView方法追加到当前mContentParent中
            mContentParent.addView(view, params);
        }
        mContentParent.requestApplyInsets();
        final Callback cb = getCallback();
        //调用CallBack接口的onContentChange来通知Activity组件视图发生了变化
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
    }
  ```
2. installDecor方法
  ```java
    //截取部分主要分析代码
    private void installDecor() {
        if (mDecor == null) {
            //如果mDecor为空则创建一个DecorView实例
            // protected DecorView generateDecor() {
            //   return new DecorView(getContext(), -1);
            // }
            mDecor = generateDecor();  
            mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            mDecor.setIsRootNamespace(true);
            if (!mInvalidatePanelMenuPosted && mInvalidatePanelMenuFeatures != 0) {
                mDecor.postOnAnimation(mInvalidatePanelMenuRunnable);
            }
        }
        if (mContentParent == null) {
            //根据窗口的风格修饰 选择对应的修饰布局文件 将id为content的FrameLayout赋值于mContentParent
            mContentParent = generateLayout(mDecor);
            ...
          }
    }
  ```
  ```java
    protected ViewGroup generateLayout(DecorView decor) {
         // Apply data from current theme.
         //根据当前style修饰相应样式

         TypedArray a = getWindowStyle();

         ...
         //一堆if判断

         // 增加窗口修饰

         int layoutResource;
         int features = getLocalFeatures();

         ...
         //根据features选择不同的窗帘修饰布局文件得到
         //把选中的窗口修饰布局文件添加到DecorView中, 指定contentParent的值
         View in = mLayoutInflater.inflate(layoutResource, null);
         decor.addView(in, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
         mContentRoot = (ViewGroup) in;

         ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
         if (contentParent == null) {
             throw new RuntimeException("Window couldn't find content container view");
         }

         ...
         return contentParent;
     }
  ```
  该方法的主要功能为 根据窗口的style为该窗口选择不同的窗口根布局文件 将mDecor作为根视图将窗口布局添加,获取id为content的FrameLayout返回给mContentParent对象  实质为阐释mDecor和mContentParent对象
3. (扩展)关于设置Activity属性需要在setContentView方法之前调用的问题

  在设置Activity属性的时候 比如requestWindowFeature(Window.FEATURE_NO_TITLE) 需要在setContentView方法之前调用
  ```java
    public boolean requestFeature(int featureId) {
        if (mContentParent != null) {
            throw new AndroidRuntimeException("requestFeature() must be called before adding content");
        }
        ...
    }
```

4. onContentChanged方法

  在PhoneWindow中没有重写getCallback相关方法 而在Window类下
  ```java
    /**
     * Return the current Callback interface for this window.
     */
    public final Callback getCallback() {
        return mCallback;
    }
  ```
  mCallback相关的赋值方法
  ```java
    /**
     * Set the Callback interface for this window, used to intercept key
     * events and other dynamic operations in the window.
     *
     * @param callback The desired Callback interface.
     */
    public void setCallback(Callback callback) {
        mCallback = callback;
    }
  ```
  setCallback方法在Activity中被使用
  ```java
    final void attach(Context context, ActivityThread aThread,
              Instrumentation instr, IBinder token, int ident,
              Application application, Intent intent, ActivityInfo info,
              CharSequence title, Activity parent, String id,
              NonConfigurationInstances lastNonConfigurationInstances,
              Configuration config, String referrer, IVoiceInteractor voiceInteractor) {
          ...
          mWindow.setCallback(this);
          ...
    }
  ```
  说明Activity实现了Window的CallBack接口 然后在Activity中找到onContentChanged方法
  ```java
    public void onContentChanged() {
    }
  ```
  对 空方法. 说明在Activity的布局改动时 (setContentView或者addContentView 方法执行完毕后会调用改方法)
   所以各种View的findViewById方法什么的可以放在这里

5. setContentView源码总结
  创建一个DecorView的对象mDector 该mDector将作为整个应用窗口的根视图

  根据根据Feature等style theme创建不同的窗口修饰布局文件 并且通过findViewById获取Activity布局文件该存放的地方

  将Activity的布局文件添加至id为content的FrameLayout内

  执行到当前页面还没有显示出来

6. Activity页面显示

  我们都知道Activity的实际开始于ActivityThread的main方法 当该方法调运完之后会调用该类的performLaunchActivity方法来创建要启动的Activity组件 这个过程中还会为该Activity组件创建窗口对象和视图对象 当组件创建完成后用过调用该类的handleResumeActivity方法将其激活

  ```java
    final void handleResumeActivity(IBinder token,
               boolean clearHide, boolean isForward, boolean reallyResume) {
                 ...
               if (!r.activity.mFinished && willBeVisible
                       && r.activity.mDecor != null && !r.hideForNow) {
                   ...
                   if (r.activity.mVisibleFromClient) {
                       r.activity.makeVisible();
                       //这里这里 通过调用Activity的makeVisible方法来显示我们通过setContentView创建的mDecor
                   }
                   ...
               }
           } else {
             ...
           }
       }
  ```
  ```java
    //Activity的makeVisible方法
    void makeVisible() {
         if (!mWindowAdded) {
             ViewManager wm = getWindowManager();
             wm.addView(mDecor, getWindow().getAttributes());
             mWindowAdded = true;
         }
         mDecor.setVisibility(View.VISIBLE);
     }
  ```
  至此通过setContentView方法设置的页面才最后显示出来

### LayoutInflater源码分析
1. 与setContentView相关

  在PhoneWindow的generateLayout中调用了     
  ```java
    View in = mLayoutInflater.inflate(layoutResource, null);
  ```

2. LayoutInflater中获取实例化方法
  ```java
    /**
     * Obtains the LayoutInflater from the given context.
     */
    public static LayoutInflater from(Context context) {
        LayoutInflater LayoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (LayoutInflater == null) {
            throw new AssertionError("LayoutInflater not found.");
        }
        return LayoutInflater;
    }
  ```

3. inflate方法相关
  ```java
    public View inflate(@LayoutRes int resource, @Nullable ViewGroup root) {
        return inflate(resource, root, root != null);
    }

    public View inflate(XmlPullParser parser, @Nullable ViewGroup root) {
      return inflate(parser, root, root != null);
    }
  ```
  ```java
    public View inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot) {
        final Resources res = getContext().getResources();
        if (DEBUG) {
            Log.d(TAG, "INFLATING from resource: \"" + res.getResourceName(resource) + "\" ("
                    + Integer.toHexString(resource) + ")");
        }

        final XmlResourceParser parser = res.getLayout(resource);
        try {
            return inflate(parser, root, attachToRoot);
        } finally {
            parser.close();
        }
    }
  ```
  最后发现都需要调用

  ```java
  public View inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot) {
          synchronized (mConstructorArgs) {
              Trace.traceBegin(Trace.TRACE_TAG_VIEW, "inflate");

              final Context inflaterContext = mContext;
              final AttributeSet attrs = Xml.asAttributeSet(parser);
              Context lastContext = (Context) mConstructorArgs[0];
              mConstructorArgs[0] = inflaterContext;
              //定义返回值 初始化传入形参 root
              View result = root;

              try {
                  // 找到根节点
                  int type;
                  while ((type = parser.next()) != XmlPullParser.START_TAG &&
                          type != XmlPullParser.END_DOCUMENT) {
                  }

                  //验证type是否为Start_Tag  保证xml文件正确
                  if (type != XmlPullParser.START_TAG) {
                      throw new InflateException(parser.getPositionDescription()
                              + ": No start tag found!");
                  }

                  //type为 root node
                  final String name = parser.getName();

                  if (DEBUG) {
                      System.out.println("**************************");
                      System.out.println("Creating root view: "
                              + name);
                      System.out.println("**************************");
                  }

                  if (TAG_MERGE.equals(name)) {
                      //处理 merge相关
                      //root需要非空 且attachToRoot为空
                      if (root == null || !attachToRoot) {
                          throw new InflateException("<merge /> can be used only with a valid "
                                  + "ViewGroup root and attachToRoot=true");
                      }
                      //递归inflate 方法调用
                      rInflate(parser, root, inflaterContext, attrs, false);
                  } else {
                      //根据tag节点创建view对象
                      final View temp = createViewFromTag(root, name, inflaterContext, attrs);

                      ViewGroup.LayoutParams params = null;

                      if (root != null) {
                          if (DEBUG) {
                              System.out.println("Creating params from root: " +
                                      root);
                          }
                          //根据root生成LayoutParams
                          params = root.generateLayoutParams(attrs);
                          if (!attachToRoot) {
                              //如果attachToRoot为flase 则调用setLayoutParams
                              temp.setLayoutParams(params);
                          }
                      }

                      if (DEBUG) {
                          System.out.println("-----> start inflating children");
                      }
                      //递归inflate剩下的children
                      rInflateChildren(parser, temp, attrs, true);

                      if (DEBUG) {
                          System.out.println("-----> done inflating children");
                      }

                      // We are supposed to attach all the views we found (int temp)
                      // to root. Do that now.
                      if (root != null && attachToRoot) {
                          //root非空且attachToRoot=true则将xml文件的root view加到形参提供的root里
                          root.addView(temp, params);
                      }

                      // Decide whether to return the root that was passed in or the
                      // top view found in xml.
                      if (root == null || !attachToRoot) {
                          //返回xml里解析的root view
                          result = temp;
                      }
                  }

              } catch (XmlPullParserException e) {
                  InflateException ex = new InflateException(e.getMessage());
                  ex.initCause(e);
                  throw ex;
              } catch (Exception e) {
                  InflateException ex = new InflateException(
                          parser.getPositionDescription()
                                  + ": " + e.getMessage());
                  ex.initCause(e);
                  throw ex;
              } finally {
                  // Don't retain static reference on context.
                  mConstructorArgs[0] = lastContext;
                  mConstructorArgs[1] = null;
              }

              Trace.traceEnd(Trace.TRACE_TAG_VIEW);
              //返回参数root或xml文件里的root view
              return result;
          }
      }

  ```

  相关inflate参数的结果


  | int resource  | ViewGroup root | boolean attachToRoot | 效果 |
  | -----:|-----:| -----:|:----------|
  | xml Id | null | - | 只创建temp的View 返回temp |
  | xml Id | parent | - | 创建temp的View 执行root.addView(temp, params) 返回root |
  | xml Id | parent | false | 创建temp的View 执行temp.setLayoutParams(params) 返回temp|
  | xml Id | parent | true| 创建temp的View 执行root.addView(temp , params) 返回root|
  | xml Id | null| false | 只创建temp的View 返回temp |
  | xml Id | null| true| 只创建temp的View 返回temp |


4. 相关方法解析
  在Inflate中多次被调用的rInflate

  ```java
     void rInflate(XmlPullParser parser, View parent, Context context,
             AttributeSet attrs, boolean finishInflate) throws XmlPullParserException, IOException {

         final int depth = parser.getDepth();
         int type;
         //XmlPullParser解析器的标准解析模式
         while (((type = parser.next()) != XmlPullParser.END_TAG ||
                 parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
             //找到start_tag节点
             if (type != XmlPullParser.START_TAG) {
                 continue;
             }
             //获取Name标记
             final String name = parser.getName();

             //private static final String TAG_REQUEST_FOCUS = "requestFocus";
             //处理requestFocus
             if (TAG_REQUEST_FOCUS.equals(name)) {
                 parseRequestFocus(parser, parent);
             // private static final String TAG_TAG = "tag";
             //处理tag
             } else if (TAG_TAG.equals(name)) {
                 parseViewTag(parser, parent, attrs);
             //private static final String TAG_INCLUDE = "include";
             //处理include
             } else if (TAG_INCLUDE.equals(name)) {
                 //如果是根节点则抛出异常
                 if (parser.getDepth() == 0) {
                     throw new InflateException("<include /> cannot be the root element");
                 }
                 parseInclude(parser, context, parent, attrs);
             //private static final String TAG_MERGE = "merge";
             //处理merge merge需要是xml中的根节点
             } else if (TAG_MERGE.equals(name)) {
                 throw new InflateException("<merge /> must be the root element");
             } else {
                 final View view = createViewFromTag(parent, name, context, attrs);
                 final ViewGroup viewGroup = (ViewGroup) parent;
                 final ViewGroup.LayoutParams params = viewGroup.generateLayoutParams(attrs);
                 rInflateChildren(parser, view, attrs, true);
                 viewGroup.addView(view, params);
             }
         }

          //parent的所有子节点都处理完毕的时候回onFinishInflate方法
         if (finishInflate) {
             parent.onFinishInflate();
         }
     }
     //可以添加自定义逻辑
      protected void onFinishInflate() {
      }
```

## View的测量 布局 绘制过程

### 测量之前的事情
View的整个绘制流程是开始于ViewRootImpl类的performTraversals方法(1k行)  根据相关设置来觉得十分要重新执行相关功能
```java
  private void performTraversals() {
    // cache mView since it is used so much below...
    final View host = mView;
    ...
    int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
    int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);
    ...
    //measure
    mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    ...
    //layout
    mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
    ...
    //draw
    mView.draw(canvas);
    ...
  }
  ```
  ```java
  private static int getRootMeasureSpec(int windowSize, int rootDimension) {
        int measureSpec;
        switch (rootDimension) {

        case ViewGroup.LayoutParams.MATCH_PARENT:
            // Window can't resize. Force root view to be windowSize.
            measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.EXACTLY);
            break;
        ...
        }
        return measureSpec;
    }
  ```
View measure整体流程图如下

![View绘制整体流程图](/View.png)

### measure源码分析

结论:
* measure的过程就是父View向子View递归调用view.measure方法 (measure中回调onMeasure方法)的过程

* measure方法是 final的 只能重载onMeasure方法

* 最顶层的DocerView的MeasureSpec由ViewRootImpl的getRootMeasureSpec方法提供 LayoutParams的参数为MATCH_PARENT specMode是EXACTLY，specSize为物理屏幕大小

* 只要是ViewGroup的子类就必须要求LayoutParams继承子MarginLayoutParams 否则无法使用layout_margin参数

* View的getMeasuredWidth()和getMeasuredHeight()方法来获取View测量的宽高，要必须保证这两个方法在onMeasure流程之后被调用才能返回有效值。



![View measure过程](/View measure过程.png)

```java
/**
 * <p>
 * This is called to find out how big a view should be. The parent supplies constraint information in the width and height parameters.
 * </p>
 *
 * <p>
 * The actual measurement work of a view is performed in
 * {@link #onMeasure(int, int)}, called by this method. Therefore, only
 * {@link #onMeasure(int, int)} can and must be overridden by subclasses.
 * </p>
 *
 *
 * @param widthMeasureSpec Horizontal space requirements as imposed by the
 *        parent
 * @param heightMeasureSpec Vertical space requirements as imposed by the
 *        parent
 *
 * @see #onMeasure(int, int)
 */
 //没舍得删这些注释  感觉重要的事情都说了   为了计算整个View树的实际大小 设置实际的高和宽 每个子View都是根据父视图和自身决定实际宽高的 在onMeasure()方法中进行实际测量.传入widthMeasureSpec和heightMeasureSpec参数来表示了父View的规格 不但传入了模式 还传入了size 而对于DecorView来说 传入的模式一般为EXACTLY模式 size对应屏幕的宽高. 所以说子View的大小是父子View共同决定的
public final void measure(int widthMeasureSpec, int heightMeasureSpec) {

               // measure ourselves, this should set the measured dimension flag back
               onMeasure(widthMeasureSpec, heightMeasureSpec);
   }
```

MeasureSpec内部类

MeasureSpec是View的内部类 int型，由高2位规格模式specMode和低30位具体尺寸specSize组成 其中specMode只有三种
* MeasureSpec.EXACTLY //确定模式，父View希望子View的大小是确定的，由specSize决定；
* MeasureSpec.AT_MOST //最多模式，父View希望子View的大小最多是specSize指定的值；
* MeasureSpec.UNSPECIFIED //未指定模式，父View完全依据子View的设计值来决定；

onMeasure()方法

```java
    /**
     * <p>
     * Measure the view and its content to determine the measured width and the
     * measured height. This method is invoked by {@link #measure(int, int)} and
     * should be overridden by subclasses to provide accurate and efficient
     * measurement of their contents.
     * </p>
     *
     * <p>
     * <strong>CONTRACT:</strong> When overriding this method, you
     * <em>must</em> call {@link #setMeasuredDimension(int, int)} to store the
     * measured width and height of this view. Failure to do so will trigger an
     * <code>IllegalStateException</code>, thrown by
     * {@link #measure(int, int)}. Calling the superclass'
     * {@link #onMeasure(int, int)} is a valid use.
     * </p>
     *
     * <p>
     * The base class implementation of measure defaults to the background size,
     * unless a larger size is allowed by the MeasureSpec. Subclasses should
     * override {@link #onMeasure(int, int)} to provide better measurements of
     * their content.
     * </p>
     *
     * <p>
     * If this method is overridden, it is the subclass's responsibility to make
     * sure the measured height and width are at least the view's minimum height
     * and width ({@link #getSuggestedMinimumHeight()} and
     * {@link #getSuggestedMinimumWidth()}).
     * </p>
     *
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent.
     *                         The requirements are encoded with
     *                         {@link android.view.View.MeasureSpec}.
     * @param heightMeasureSpec vertical space requirements as imposed by the parent.
     *                         The requirements are encoded with
     *                         {@link android.view.View.MeasureSpec}.
     *
     * @see #getMeasuredWidth()
     * @see #getMeasuredHeight()
     * @see #setMeasuredDimension(int, int)
     * @see #getSuggestedMinimumHeight()
     * @see #getSuggestedMinimumWidth()
     * @see android.view.View.MeasureSpec#getMode(int)
     * @see android.view.View.MeasureSpec#getSize(int)
     */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }
```
getDefaultSize方法相关
```java
  public static int getDefaultSize(int size, int measureSpec) {
    int result = size;
    //通过measureSpec得到mode和size
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);

    switch (specMode) {
    case MeasureSpec.UNSPECIFIED:
        result = size;
        break;
    case MeasureSpec.AT_MOST:
    case MeasureSpec.EXACTLY:
        result = specSize;
        break;
    }
    return result;
  }

  //最小宽度和高度由View的Background尺寸和View的minXXX共同决定
  protected int getSuggestedMinimumHeight() {
      return (mBackground == null) ? mMinHeight : max(mMinHeight, mBackground.getMinimumHeight());

  }
  protected int getSuggestedMinimumWidth() {
      return (mBackground == null) ? mMinWidth : max(mMinWidth, mBackground.getMinimumWidth());
  }
```
setMeasuredDimension方法 对View的成员变量measuredWidth和measuredHeight变量赋值 也就是说该方法最终决定了View的大小
```java
  protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
      boolean optical = isLayoutModeOptical(this);
      if (optical != isLayoutModeOptical(mParent)) {
          Insets insets = getOpticalInsets();
          int opticalWidth  = insets.left + insets.right;
          int opticalHeight = insets.top  + insets.bottom;

          measuredWidth  += optical ? opticalWidth  : -opticalWidth;
          measuredHeight += optical ? opticalHeight : -opticalHeight;
      }
      setMeasuredDimensionRaw(measuredWidth, measuredHeight);
  }

  public boolean isLayoutRequested() {
    return (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;
  }

  private void setMeasuredDimensionRaw(int measuredWidth, int measuredHeight) {
    mMeasuredWidth = measuredWidth;
    mMeasuredHeight = measuredHeight;

    mPrivateFlags |= PFLAG_MEASURED_DIMENSION_SET;
  }
```

至此一次最基础的View的measure过程就完成了  但是由于View可以嵌套  所以measure是递归传递的所以ViewGroup中需要对其子类进行measure过程 measureChildren方法实质为循环调用measureChild方法

而measureChild和measureChildWithMargins的区别是后者将margin和padding也作为了子视图的大小

一下分析measureChildWithMargins方法
```java
  protected void measureChildWithMargins(View child,
          int parentWidthMeasureSpec, int widthUsed,
          int parentHeightMeasureSpec, int heightUsed) {
      //获取当前子视图的LayoutParams
      final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
      //设定子View的测量规格
      final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
              mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
                      + widthUsed, lp.width);
      final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
              mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin
                      + heightUsed, lp.height);
      //子view的继续调用
      child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
  }

  //在getChildMeasureSpec中通过父View和本身的模式共同决定当前View的size
  public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
        //获取当前父View的mode和size
        int specMode = MeasureSpec.getMode(spec);
        int specSize = MeasureSpec.getSize(spec);
        //获取父View的的剩余大小
        int size = Math.max(0, specSize - padding);
        //定义结果变量
        int resultSize = 0;
        int resultMode = 0;
        //根据对应的mode做处理
        //通过父View和本身的模式共同决定当前View的size
        switch (specMode) {
        // Parent has imposed an exact size on us
        case MeasureSpec.EXACTLY:
            if (childDimension >= 0) {
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.MATCH_PARENT) {
                // Child wants to be our size. So be it.
                resultSize = size;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                // Child wants to determine its own size. It can't be
                // bigger than us.
                resultSize = size;
                resultMode = MeasureSpec.AT_MOST;
            }
            break;

        // Parent has imposed a maximum size on us
        case MeasureSpec.AT_MOST:
            if (childDimension >= 0) {
                // Child wants a specific size... so be it
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.MATCH_PARENT) {
                // Child wants to be our size, but our size is not fixed.
                // Constrain child to not be bigger than us.
                resultSize = size;
                resultMode = MeasureSpec.AT_MOST;
            } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                // Child wants to determine its own size. It can't be
                // bigger than us.
                resultSize = size;
                resultMode = MeasureSpec.AT_MOST;
            }
            break;

        // Parent asked to see how big we want to be
        case MeasureSpec.UNSPECIFIED:
            if (childDimension >= 0) {
                // Child wants a specific size... let him have it
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.MATCH_PARENT) {
                // Child wants to be our size... find out how big it should
                // be
                resultSize = View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
                resultMode = MeasureSpec.UNSPECIFIED;
            } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                // Child wants to determine its own size.... find out how
                // big it should be
                resultSize = View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
                resultMode = MeasureSpec.UNSPECIFIED;
            }
            break;
        }
        //将size和mode整合为MeasureSpec模式后返回
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

```

### layout源码分析
View layout整体流程与measure过程基本一样

结论:
* 需要根据ViewGroup本身的情况讨论 LinearLayout下会更看重子View的height和width 来安排对应位置 而RelativeLayout则更加关注子View的left right top bottom值 并且优先级高于width和height 甚至在部分自定义ViewGroup中 measure可能是无用的   直接使用layout方法来设置子View的位置也可以
* ViewGroup需要实现自己的layout逻辑
* layout_XXX中的各个熟悉都是针对子View的父ViewGroup的
* 同样使用View的getWidth()和getHeight()方法来获取View测量的宽高 必须保证这两个方法在onLayout流程之后被调用才能返回有效值

```java
  /**
     * Assign a size and position to a view and all of its
     * descendants
     *
     * <p>This is the second phase of the layout mechanism.
     * (The first is measuring). In this phase, each parent calls
     * layout on all of its children to position them.
     * This is typically done using the child measurements
     * that were stored in the measure pass().</p>
     *
     * <p>Derived classes should not override this method.
     * Derived classes with children should override
     * onLayout. In that method, they should
     * call layout on each of their children.</p>
     *
     * @param l Left position, relative to parent
     * @param t Top position, relative to parent
     * @param r Right position, relative to parent
     * @param b Bottom position, relative to parent
     */

     //同样注解写的很好了  分派给他和他的所有的子视图大小和位置
    @SuppressWarnings({"unchecked"})
    public void layout(int l, int t, int r, int b) {
        if ((mPrivateFlags3 & PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT) != 0) {
            onMeasure(mOldWidthMeasureSpec, mOldHeightMeasureSpec);
            mPrivateFlags3 &= ~PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT;
        }
         //调用setFrame方法把参数分别赋值于
        int oldL = mLeft;
        int oldT = mTop;
        int oldB = mBottom;
        int oldR = mRight;
        //判断view的位置是否发生过变化 , 确定是否对当前view重新layout
        boolean changed = isLayoutModeOptical(mParent) ?
                setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);


        if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
            //调用onLayout
            onLayout(changed, l, t, r, b);
            mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;
            ListenerInfo li = mListenerInfo;
            if (li != null && li.mOnLayoutChangeListeners != null) {
                ArrayList<OnLayoutChangeListener> listenersCopy =
                        (ArrayList<OnLayoutChangeListener>)li.mOnLayoutChangeListeners.clone();
                int numListeners = listenersCopy.size();
                for (int i = 0; i < numListeners; ++i) {
                    listenersCopy.get(i).onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB);
                }
            }
        }

        mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
        mPrivateFlags3 |= PFLAG3_IS_LAID_OUT;
    }
```

onLyayout方法
```java
  View中
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
  }
  ViewGroup中
  protected abstract void onLayout(boolean changed,
        int l, int t, int r, int b);
```

均是空方法  后面会就LinearLayout和RelativeLayout源码进行分析

### draw源码分析

View的draw流程图如下

![Viewdraw](View draw流程.png)

结论:
* View需要在子类中实现onDraw的过程
* 在ViewGroup中 会调用其子View的方法 顺序与子view的添加顺序一致


draw的源码也很长 但是官方也给出给出了draw的过程
```java
  public void draw(Canvas canvas) {
     ...
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
      */

     // Step 1, draw the background, if needed
     ...
     if (!dirtyOpaque) {
         drawBackground(canvas);
     }

     // skip step 2 & 5 if possible (common case)
     ...

     // Step 2, save the canvas' layers
     ...
         if (drawTop) {
             canvas.saveLayer(left, top, right, top + length, null, flags);
         }
     ...

     // Step 3, draw the content
     if (!dirtyOpaque) onDraw(canvas);

     // Step 4, draw the children
     dispatchDraw(canvas);

     // Step 5, draw the fade effect and restore layers
     ...
     if (drawTop) {
         matrix.setScale(1, fadeHeight * topFadeStrength);
         matrix.postTranslate(left, top);
         fade.setLocalMatrix(matrix);
         p.setShader(fade);
         canvas.drawRect(left, top, right, top + length, p);
     }
     ...

     // Step 6, draw decorations (scrollbars)
     onDrawScrollBars(canvas);
     ...
 }
```

#### Step 1, draw the background, if needed
```java
  // Step 1, draw the background, if needed
  //如果需要的话绘制背景

  if (!dirtyOpaque) {
      drawBackground(canvas);
  }
```

```java
  private void drawBackground(Canvas canvas) {

    	//通过xml中属性background或者代码中setBackGroundColor\setBackgroundResource等方法赋值的背景drawable
        final Drawable background = mBackground;
        if (background == null) {
            return;
        }

        //根据layout中确定的view位置来设置背景的绘制区域
        setBackgroundBounds();


        // 如果需要的话使用显示列表
        //canvas.isHardwareAccelerated() 硬件加速判定
        //硬件加速时会将图层缓存到GPU上 而不是重绘View的每一层
        if (canvas.isHardwareAccelerated() && mAttachInfo != null
                && mAttachInfo.mHardwareRenderer != null) {
            mBackgroundRenderNode = getDrawableRenderNode(background, mBackgroundRenderNode);

            final RenderNode renderNode = mBackgroundRenderNode;
            if (renderNode != null && renderNode.isValid()) {
                setBackgroundRenderNodeProperties(renderNode);
                ((DisplayListCanvas) canvas).drawRenderNode(renderNode);
                return;
            }
        }

        final int scrollX = mScrollX;
        final int scrollY = mScrollY;
        //调用Drawable的draw方法来完成背景的绘制工作
        if ((scrollX | scrollY) == 0) {
            background.draw(canvas);
        } else {
            canvas.translate(scrollX, scrollY);
            background.draw(canvas);
            canvas.translate(-scrollX, -scrollY);
        }
    }


    void setBackgroundBounds() {
    if (mBackgroundSizeChanged && mBackground != null) {
        mBackground.setBounds(0, 0,  mRight - mLeft, mBottom - mTop);
        mBackgroundSizeChanged = false;
        rebuildOutline();
    }
  }
```


#### Step 2, save the canvas' layers
```java
  // Step 2, save the canvas' layers
  //保存绘制图层

         if (drawTop) {
             canvas.saveLayer(left, top, right, top + length, null, flags);
         }

```

#### // Step 3, draw the content
```java
  // Step 3, draw the content
  //对View的内容进行绘制
  if (!dirtyOpaque) onDraw(canvas);
```
```java
  /**
  * Implement this to do your drawing.
  *
  * @param canvas the canvas on which the background will be drawn
  */
  //onDraw也是空方法需要子类根据自身去实现相应的
  protected void onDraw(Canvas canvas) {
  }

```

#### Step 4, draw the children
```java
  // Step 4, draw the children
  //绘制其子View
  dispatchDraw(canvas);
```

```java
  /**
   * Called by draw to draw the child views. This may be overridden
   * by derived classes to gain control just before its children are drawn
   * (but after its own view has been drawn).
   * @param canvas the canvas on which to draw the view
   */
  protected void dispatchDraw(Canvas canvas) {
  //dispatchDraw同样空方法 与onDraw不同的是dispatchDraw在ViewGroup中被重写
  }
```

ViewGroup
```java
  //dispatchDraw方法中根据子View的不同情况 包括但不只包括该View是否显示 是否有进入或消失动画等进行了部分的调整
  protected void dispatchDraw(Canvas canvas) {
      ...
        more |= drawChild(canvas, transientChild, drawingTime);
      ...    
  }

  protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    return child.draw(canvas, this, drawingTime);
  }
```
#### Step 5, draw the fade effect and restore layers

```java
  // Step 5, draw the fade effect and restore layers
  //绘制过度效果和恢复图层
  if (drawTop) {
      matrix.setScale(1, fadeHeight * topFadeStrength);
      matrix.postTranslate(left, top);
      fade.setLocalMatrix(matrix);
      p.setShader(fade);
      canvas.drawRect(left, top, right, top + length, p);
  }
```

####  Step 6, draw decorations (scrollbars)
```java
  // Step 6, draw decorations (scrollbars)
  //对滚动条进行绘制
  onDrawScrollBars(canvas);
```
