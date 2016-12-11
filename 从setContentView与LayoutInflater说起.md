
## 从setContentView与LayoutInflater说起
### setContentView分析
#### 相关关系

  ![相关关系图](/image_1.png)  

  Activity中有Window成员 实例化为PhoneWindow PhoneWindow是抽象Window类的实现类

  Window提供了绘制窗口的通用API PhoneWindow中包含了DecorView对象 是所有窗口(Activity界面)的根View

  具体的构如下

  ![View层级分析](/View层级分析.png)

  具体的可以通过hierarchyviewer工具分析一下



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
  * 创建一个DecorView的对象mDector 该mDector将作为整个应用窗口的根视图
  *  根据根据Feature等style theme创建不同的窗口修饰布局文件 并且通过findViewById获取Activity布局文件该存放的地方
  *  将Activity的布局文件添加至id为content的FrameLayout内
  *  执行到当前页面还没有显示出来

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
  ![inflate参数.png](/inflate参数.png)

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
