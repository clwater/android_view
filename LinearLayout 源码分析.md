## LinearLayout 源码分析

### measure过程
#### 主要过程
* 根据布局方向选择measure过程分支
* 初始化相关变量
* 对View进行第一次测量
* mTotalLength的再次测量
* 二次测量部分View和对为测量的子View进行测量


```java
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      //判断布局方向
      if (mOrientation == VERTICAL) {
          measureVertical(widthMeasureSpec, heightMeasureSpec);
      } else {
          measureHorizontal(widthMeasureSpec, heightMeasureSpec);
      }
  }
```
measureVertical和measureHorizontal只是布局方向上的区别 以下主要分析measureVertical方法

#### 初始化相关变量
```java
  //mTotalLength是记录内部使用的高度也就是子View的高度和 而不是LinearLayout的高度
  mTotalLength = 0;
  //子视图的最大宽度(不包括layout_weight>0的子View)
  int maxWidth = 0;
  int childState = 0;
  int alternativeMaxWidth = 0;
  //子视图的最大宽度(仅包含layout_weight>0的子View)
  int weightedMaxWidth = 0;
  //子视图是否均为fillParent 用于判断是否需要重新计算
  boolean allFillParent = true;
  //权重值的总和
  float totalWeight = 0;
  //子View的数量(统一级别下)
  final int count = getVirtualChildCount();
  //高度宽度模式
  final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
  final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
  //子View的宽度是否需要由父View决定
  boolean matchWidth = false;
  boolean skippedMeasure = false;
  //第几个子View的baseLine作为LinearLayout的基准线
  final int baselineChildIndex = mBaselineAlignedChildIndex;  
  //mUseLargestChild为是否使用最大子元素的尺寸作为标准再次测量
  final boolean useLargestChild = mUseLargestChild;
  //子View中最高高度
  int largestChildHeight = Integer.MIN_VALUE;
```

#### 第一次测量
```java
  // See how tall everyone is. Also remember max width.
  for (int i = 0; i < count; ++i) {
      final View child = getVirtualChildAt(i);

      // 测量为null的子视图的高度
      // measureNullChild() 暂时返回 0 便于扩展
      if (child == null) {
          mTotalLength += measureNullChild(i);
          continue;
      }
      //Visibility为Gone的时候跳过该View
      // getChildrenSkipCount()方法同样返回0 便于扩展
      if (child.getVisibility() == View.GONE) {
         i += getChildrenSkipCount(child, i);
         continue;
      }
      //根据showDivider的值(通过hasDividerBeforeChildAt()) 来决定当前子View是否需要添加分割线的高度
      if (hasDividerBeforeChildAt(i)) {
          mTotalLength += mDividerHeight;
      }

      //会将子view的LayoutParams强转为父View的LayoutParams类型
      LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)child.getLayoutParams();

      totalWeight += lp.weight;

      if (heightMode == MeasureSpec.EXACTLY && lp.height == 0 && lp.weight > 0) {
          // 满足该条件的话 不需要现在计算该子视图的高度 测量工作会在之后进行
          // 若子View的height=0 且weight> 0 则说明该View希望使用的是LinearLayout的剩余空间
          // LinearLayout是EXACTLY模式的说明LinearLayout高度已经确定 不需要依赖子View的测量结果来计算自己 就无需测量该子View

          final int totalLength = mTotalLength;
          mTotalLength = Math.max(totalLength, totalLength + lp.topMargin + lp.bottomMargin);
          skippedMeasure = true;
      } else {
          //测量子View

          int oldHeight = Integer.MIN_VALUE;

          //当前View的height=0 且weight> 0 则说明该LinearLayout的高度需要靠子View测量(不需要的在上面分支处理了)
          //将子View的高度设为-1 防止子View高度为0
          if (lp.height == 0 && lp.weight > 0) {
              oldHeight = 0;
              lp.height = LayoutParams.WRAP_CONTENT;
          }

          //调用子View的measureChildWithMargins() 对子View进行测量
          //第四个参数表示当前已使用的宽度  因为是竖直模式 所以为0
          //最后一个参数表示已使用的高度 如果之前的子View或者当前的View有weight属性 则当前子视图使用 LinearLayout 的所有高度 已使用的高度为0
          measureChildBeforeLayout(child, i, widthMeasureSpec, 0, heightMeasureSpec,
                 totalWeight == 0 ? mTotalLength : 0);

          if (oldHeight != Integer.MIN_VALUE) {
             //测量完成后 重置子View高度
             lp.height = oldHeight;
          }

          final int childHeight = child.getMeasuredHeight();
          final int totalLength = mTotalLength;
          // 比较child测量前后总高度 取较大值
          ///getNextLocationOffset() 返回0 便于扩展
          mTotalLength = Math.max(totalLength, totalLength + childHeight + lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));
          // 设置最高子视图大小
          if (useLargestChild) {
              largestChildHeight = Math.max(childHeight, largestChildHeight);
          }
      }

       // mBaselineChildTop 表示指定的 baseline 的子视图的顶部高度
      if ((baselineChildIndex >= 0) && (baselineChildIndex == i + 1)) {
         mBaselineChildTop = mTotalLength;
      }

      // 设置为 baseline 的子视图的前面不允许设置 weiget 属性
      if (i < baselineChildIndex && lp.weight > 0) {
          throw new RuntimeException("A child of LinearLayout with index "
                  + "less than mBaselineAlignedChildIndex has weight > 0, which "
                  + "won't work.  Either remove the weight, or don't set "
                  + "mBaselineAlignedChildIndex.");
      }

      // 宽度测量相关

      boolean matchWidthLocally = false;

      //当LinearLayout非EXACTLY模式 并且自View为MATCH_PARENT时
      //设置matchWidth和matchWidthLocally为true
      //该子View占据LinearLayout水平方向上所有空间

      if (widthMode != MeasureSpec.EXACTLY && lp.width == LayoutParams.MATCH_PARENT) {
          matchWidth = true;
          matchWidthLocally = true;
      }

      final int margin = lp.leftMargin + lp.rightMargin;
      final int measuredWidth = child.getMeasuredWidth() + margin;

      //对一堆变量赋值
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
```

#### 二次测量mTotalLength

```java
  //根据hasDividerBeforeChildAt得到showDivider的值是否为end 来判断是否需要加上divider的高度
  if (mTotalLength > 0 && hasDividerBeforeChildAt(count))
      mTotalLength += mDividerHeight;
  }
  //如果高度测量模式为AT_MOST或者UNSPECIFIED 则进行二次测量 且设置了measureWithLargestChild
  if (useLargestChild && (heightMode == MeasureSpec.AT_MOST ||
      heightMode == MeasureSpec.UNSPECIFIED)) {
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
          // 计算所有子View的高度之和
          final int totalLength = mTotalLength;
          mTotalLength = Math.max(totalLength, totalLength + largestChildHeight +
                  lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));
      }
  }
```
就是需要useLargestChild

而 mUseLargestChild = a.getBoolean(R.styleable.LinearLayout_measureWithLargestChild, false);

就是说仅在LinearLayout的measureWithLargestChild属性设置为True时(默认为false)才可能出现某个child被二次测量

实例如下

![LinearLayout二次测量](/LinearLayout二次测量.png)



#### 二次测量部分View和对为测量的子View进行测量

```java

   //加上padding的值
   mTotalLength += mPaddingTop + mPaddingBottom;
   int heightSize = mTotalLength;
   //minHeight和当前使用的高度比较取较大值
   heightSize = Math.max(heightSize, getSuggestedMinimumHeight());

   //根据heightMeasureSpec协助计算heightSizeAndState的大小
   //resolveSizeAndState方法之后会分析
   int heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0);
   heightSize = heightSizeAndState & MEASURED_SIZE_MASK;

   // Either expand children with weight to take up available space or
   // shrink them if they extend beyond our current bounds. If we skipped
   // measurement on any children, we need to measure them now.



   //delta为额外的空间 及LinearLayout中未被分配的空间(可以为负)
   int delta = heightSize - mTotalLength;
   if (skippedMeasure || delta != 0 && totalWeight > 0.0f) {
       //skippedMeasure为第一次测量下对跳过测量的子View设置的
       //weightSum为权重和 如果设置了总权重则使用我们所设置的  如果没有则使用子View的weight和
       float weightSum = mWeightSum > 0.0f ? mWeightSum : totalWeight;

       mTotalLength = 0;
       //测量什么的
       for (int i = 0; i < count; ++i) {
           final View child = getVirtualChildAt(i);

           if (child.getVisibility() == View.GONE) {
               continue;
           }

           LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();

           float childExtra = lp.weight;
           if (childExtra > 0) {
               // Child said it could absorb extra space -- give him his share
               //计算weight属性分配的大小
               int share = (int) (childExtra * delta / weightSum);
               //权重和减去已经分配权重
               weightSum -= childExtra;
               //剩余高度减去分配的高度
               delta -= share;

               final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                       mPaddingLeft + mPaddingRight +
                               lp.leftMargin + lp.rightMargin, lp.width);

               // TODO: Use a field like lp.isMeasured to figure out if this
               // child has been previously measured


               if ((lp.height != 0) || (heightMode != MeasureSpec.EXACTLY)) {
                   //子视图已经被测量过
                   //非EXACTLY view需要加上share
                   int childHeight = child.getMeasuredHeight() + share;
                   if (childHeight < 0) {
                       childHeight = 0;
                   }
                   //重新测量View
                   child.measure(childWidthMeasureSpec,
                           MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
               } else {
                   //如果当前是EXACTLY模式 说明没有被测量 需要进行测量
                   //子视图首次被测量
                   //EXACTLY模式下 将weight占比的高度分配给子View    
                   child.measure(childWidthMeasureSpec,
                           MeasureSpec.makeMeasureSpec(share > 0 ? share : 0,
                                   MeasureSpec.EXACTLY));
               }

               // Child may now not fit in vertical dimension.
               childState = combineMeasuredStates(childState, child.getMeasuredState()
                       & (MEASURED_STATE_MASK>>MEASURED_HEIGHT_STATE_SHIFT));
           }

          //处理子视图宽度
           final int margin =  lp.leftMargin + lp.rightMargin;
           final int measuredWidth = child.getMeasuredWidth() + margin;
           maxWidth = Math.max(maxWidth, measuredWidth);

           boolean matchWidthLocally = widthMode != MeasureSpec.EXACTLY &&
                   lp.width == LayoutParams.MATCH_PARENT;

           alternativeMaxWidth = Math.max(alternativeMaxWidth,
                   matchWidthLocally ? margin : measuredWidth);

           allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT;

           final int totalLength = mTotalLength;
           mTotalLength = Math.max(totalLength, totalLength + child.getMeasuredHeight() +
                   lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));
       }

       // Add in our padding
       mTotalLength += mPaddingTop + mPaddingBottom;
       // TODO: Should we recompute the heightSpec based on the new total length?
   } else {
       alternativeMaxWidth = Math.max(alternativeMaxWidth,
                                      weightedMaxWidth);


       // We have no limit, so make all weighted views as tall as the largest child.
       // Children will have already been measured once.
       if (useLargestChild && heightMode != MeasureSpec.EXACTLY) {
           for (int i = 0; i < count; i++) {
               final View child = getVirtualChildAt(i);

               if (child == null || child.getVisibility() == View.GONE) {
                   continue;
               }

               final LinearLayout.LayoutParams lp =
                       (LinearLayout.LayoutParams) child.getLayoutParams();

               float childExtra = lp.weight;
               if (childExtra > 0) {
                  //使用最大子视图高度测量
                   child.measure(
                           MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(),
                                   MeasureSpec.EXACTLY),
                           MeasureSpec.makeMeasureSpec(largestChildHeight,
                                   MeasureSpec.EXACTLY));
               }
           }
       }
   }

   if (!allFillParent && widthMode != MeasureSpec.EXACTLY) {
       maxWidth = alternativeMaxWidth;
   }

   maxWidth += mPaddingLeft + mPaddingRight;

   // Check against our minimum width
   maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

   setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
           heightSizeAndState);

   if (matchWidth) {
       forceUniformWidth(count, heightMeasureSpec);
   }
```
resolveSizeAndState方法 定义在View中
```java
/**
   * Utility to reconcile a desired size and state, with constraints imposed
   * by a MeasureSpec. Will take the desired size, unless a different size
   * is imposed by the constraints. The returned value is a compound integer,
   * with the resolved size in the {@link #MEASURED_SIZE_MASK} bits and
   * optionally the bit {@link #MEASURED_STATE_TOO_SMALL} set if the
   * resulting size is smaller than the size the view wants to be.
   *
   * @param size How big the view wants to be.
   * @param measureSpec Constraints imposed by the parent.
   * @param childMeasuredState Size information bit mask for the view's
   *                           children.
   * @return Size information bit mask as defined by
   *         {@link #MEASURED_SIZE_MASK} and
   *         {@link #MEASURED_STATE_TOO_SMALL}.
   */
  public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
      final int specMode = MeasureSpec.getMode(measureSpec);
      final int specSize = MeasureSpec.getSize(measureSpec);
      final int result;
      switch (specMode) {
          case MeasureSpec.AT_MOST:
              if (specSize < size) {
                  result = specSize | MEASURED_STATE_TOO_SMALL;
              } else {
                  result = size;
              }
              break;
          case MeasureSpec.EXACTLY:
              result = specSize;
              break;
          case MeasureSpec.UNSPECIFIED:
          default:
              result = size;
      }
      return result | (childMeasuredState & MEASURED_STATE_MASK);
  }
```

###### delta为负的相关解析
相关代码及效果如下

![负delta.png](/负delta.png)

根据之前的measure流程分析一下

* 相关变量初始化
* 第一次测量  两个子TextView都会被测量 TextView1.height = TextView1.height = 500dp 则mToatalLength为1000dp
* mToatalLength再次测量跳过
* 计算delta delta = heightSize - mTotalLength 根据resolveSizeAndState方法 父LinearLayout是EXACTLY模式 所以最终heightSize为500dp delta = -500dp
* 根据weight分配剩余空间 TextView1.height = 500 + 1 / 5 * (- 500) = 400 dp
  TextView2.height = 500 + 4 / 5 * (- 500) = 100 dp


### layout过程


```java
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (mOrientation == VERTICAL) {
        layoutVertical(l, t, r, b);
    } else {
        layoutHorizontal(l, t, r, b);
    }
  }
```
我们可以看出 同样是分成水平和竖直两个方向的 同样分析竖直 方向下的layout过程

```java
/**
    * Position the children during a layout pass if the orientation of this
    * LinearLayout is set to {@link #VERTICAL}.
    *
    * @see #getOrientation()
    * @see #setOrientation(int)
    * @see #onLayout(boolean, int, int, int, int)
    * @param left
    * @param top
    * @param right
    * @param bottom
    */
   void layoutVertical(int left, int top, int right, int bottom) {
       final int paddingLeft = mPaddingLeft;

       int childTop;
       int childLeft;

       //父View默认子View的宽度
       final int width = right - left;
       //子View的右侧默认位置
       int childRight = width - mPaddingRight;

       // 子View的可用空间大小
       int childSpace = width - paddingLeft - mPaddingRight;

       //子View的个数
       final int count = getVirtualChildCount();

       final int majorGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
       final int minorGravity = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;

       //根据LinearLayout设置的对其方式 设置第一个子View的Top值
       switch (majorGravity) {
          case Gravity.BOTTOM:
              // mTotalLength contains the padding already
              childTop = mPaddingTop + bottom - top - mTotalLength;
              break;

              // mTotalLength contains the padding already
          case Gravity.CENTER_VERTICAL:
              childTop = mPaddingTop + (bottom - top - mTotalLength) / 2;
              break;

          case Gravity.TOP:
          default:
              childTop = mPaddingTop;
              break;
       }

       //遍历各个子View
       for (int i = 0; i < count; i++) {
           final View child = getVirtualChildAt(i);
           if (child == null) {
               childTop += measureNullChild(i);
           } else if (child.getVisibility() != GONE) {
                //LinearLayout中子View的宽和高有measure过程决定
               final int childWidth = child.getMeasuredWidth();
               final int childHeight = child.getMeasuredHeight();
               //获取子View的LayoutParams
               final LinearLayout.LayoutParams lp =
                       (LinearLayout.LayoutParams) child.getLayoutParams();

               int gravity = lp.gravity;
               if (gravity < 0) {
                   gravity = minorGravity;
               }
               final int layoutDirection = getLayoutDirection();
               final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
               //根据子View的对其方式设置Left值
               switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                   case Gravity.CENTER_HORIZONTAL:
                       childLeft = paddingLeft + ((childSpace - childWidth) / 2)
                               + lp.leftMargin - lp.rightMargin;
                       break;

                   case Gravity.RIGHT:
                       childLeft = childRight - childWidth - lp.rightMargin;
                       break;

                   case Gravity.LEFT:
                   default:
                       childLeft = paddingLeft + lp.leftMargin;
                       break;
               }
               //如果有分割线 添加分割线的高度
               if (hasDividerBeforeChildAt(i)) {
                   childTop += mDividerHeight;
               }
               //子View的top修改
               childTop += lp.topMargin;
               //用setChildFrame()方法设置子控件控件的在父控件上的坐标轴
               setChildFrame(child, childLeft, childTop + getLocationOffset(child),
                       childWidth, childHeight);
               childTop += childHeight + lp.bottomMargin + getNextLocationOffset(child);

               i += getChildrenSkipCount(child, i);
           }
       }
   }
```

#### draw 源码分析
```java
  protected void onDraw(Canvas canvas) {
    if (mDivider == null) {
        return;
    }

    if (mOrientation == VERTICAL) {
        drawDividersVertical(canvas);
    } else {
        drawDividersHorizontal(canvas);
    }
  }
```
同样主要分析垂直方向的处理

```java

  void drawDividersVertical(Canvas canvas) {
      final int count = getVirtualChildCount();
      //根据计算好的坐标绘制对应的子View
      for (int i = 0; i < count; i++) {
          final View child = getVirtualChildAt(i);

          if (child != null && child.getVisibility() != GONE) {
              if (hasDividerBeforeChildAt(i)) {
                  final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                  final int top = child.getTop() - lp.topMargin - mDividerHeight;
                  drawHorizontalDivider(canvas, top);
              }
          }
      }
      //绘制分割线
      if (hasDividerBeforeChildAt(count)) {
          final View child = getLastNonGoneChild();
          int bottom = 0;
          if (child == null) {
              bottom = getHeight() - getPaddingBottom() - mDividerHeight;
          } else {
              final LayoutParams lp = (LayoutParams) child.getLayoutParams();
              bottom = child.getBottom() + lp.bottomMargin;
          }
          drawHorizontalDivider(canvas, bottom);
      }
  }

  void drawHorizontalDivider(Canvas canvas, int top) {
    mDivider.setBounds(getPaddingLeft() + mDividerPadding, top,
            getWidth() - getPaddingRight() - mDividerPadding, top + mDividerHeight);
    mDivider.draw(canvas);
  }
```
