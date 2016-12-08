## LinearLayout 源码分析

### measure过程
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

#### 二次测量

```java
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

二次测量条件中有这么一个条件  就是需要useLargestChild

而mUseLargestChild = a.getBoolean(R.styleable.LinearLayout_measureWithLargestChild, false);

就是说仅在LinearLayout的measureWithLargestChild属性设置为True时(默认为false)才可能出现某个child被二次测量

实例如下

![LinearLayout二次测量](/LinearLayout二次测量.png)

由此来看不是所有的weight都会被二次测量


#### 测量未必测量的子View(Weight)

```java

      //首先计算高度
      int delta = heightSize - mTotalLength;
      if (skippedMeasure || delta != 0 && totalWeight > 0.0f) {
          //weightSum 如果设置了weightSum 则试用设置的weightSum而不是默认的
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
                  //子空间weigt 占比 * 剩余高度
                  int share = (int) (childExtra * delta / weightSum);
                  weightSum -= childExtra;
                  //剩余高度减去分配的高度
                  delta -= share;

                  final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                          mPaddingLeft + mPaddingRight +
                                  lp.leftMargin + lp.rightMargin, lp.width);

                  // TODO: Use a field like lp.isMeasured to figure out if this
                  // child has been previously measured

                  //如果当前是EXACTLY模式 说明没有被测量 需要进行测量
                  if ((lp.height != 0) || (heightMode != MeasureSpec.EXACTLY)) {
                      // child was measured once already above...
                      // base new measurement on stored values

                      //非EXACTLY view需要加上share
                      int childHeight = child.getMeasuredHeight() + share;
                      if (childHeight < 0) {
                          childHeight = 0;
                      }
                      //重新测量View
                      child.measure(childWidthMeasureSpec,
                              MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
                  } else {
                      // child was skipped in the loop above.
                      // Measure for this first time here  

                      //EXACTLY模式下 将weight占比的高度分配给子View    
                      child.measure(childWidthMeasureSpec,
                              MeasureSpec.makeMeasureSpec(share > 0 ? share : 0,
                                      MeasureSpec.EXACTLY));
                  }

                  // Child may now not fit in vertical dimension.
                  childState = combineMeasuredStates(childState, child.getMeasuredState()
                          & (MEASURED_STATE_MASK>>MEASURED_HEIGHT_STATE_SHIFT));
              }

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
                      child.measure(
                              MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(),
                                      MeasureSpec.EXACTLY),
                              MeasureSpec.makeMeasureSpec(largestChildHeight,
                                      MeasureSpec.EXACTLY));
                  }
              }
          }
      }
```



```java



        //首先计算高度
        int delta = heightSize - mTotalLength;
        if (skippedMeasure || delta != 0 && totalWeight > 0.0f) {
            //weightSum 如果设置了weightSum 则试用设置的weightSum而不是默认的
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
                    //子空间weigt 占比 * 剩余高度
                    int share = (int) (childExtra * delta / weightSum);
                    weightSum -= childExtra;
                    //剩余高度减去分配的高度
                    delta -= share;

                    final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            mPaddingLeft + mPaddingRight +
                                    lp.leftMargin + lp.rightMargin, lp.width);

                    // TODO: Use a field like lp.isMeasured to figure out if this
                    // child has been previously measured

                    //如果当前是EXACTLY模式 说明没有被测量 需要进行测量
                    if ((lp.height != 0) || (heightMode != MeasureSpec.EXACTLY)) {
                        // child was measured once already above...
                        // base new measurement on stored values

                        //非EXACTLY view需要加上share
                        int childHeight = child.getMeasuredHeight() + share;
                        if (childHeight < 0) {
                            childHeight = 0;
                        }
                        //重新测量View
                        child.measure(childWidthMeasureSpec,
                                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
                    } else {
                        // child was skipped in the loop above.
                        // Measure for this first time here  

                        //EXACTLY模式下 将weight占比的高度分配给子View    
                        child.measure(childWidthMeasureSpec,
                                MeasureSpec.makeMeasureSpec(share > 0 ? share : 0,
                                        MeasureSpec.EXACTLY));
                    }

                    // Child may now not fit in vertical dimension.
                    childState = combineMeasuredStates(childState, child.getMeasuredState()
                            & (MEASURED_STATE_MASK>>MEASURED_HEIGHT_STATE_SHIFT));
                }

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
    }

```

## RelativeLayout  源码分析
> 继承自ViewGroup 没有重载onDraw方法 内部子View又是相对 只要计算出View的坐标 layout过程同样简单

### measure过程
#### 主要过程
1. 将内部View根据纵向关系和横向关系排序
2. 初始化相关变量
3. 遍历水平关系的View
4. 遍历竖直关系的View
5. baseline计算
6. 宽度和高度修正

#### 1 将内部View根据纵向关系和横向关系排序
>layout_toRightOf 为横向关系  layout_below为纵向关系

```java
    //首先会根据mDirtyHierarchy的值判断是否需要将子View重新排序
    if (mDirtyHierarchy) {
        mDirtyHierarchy = false;
        sortChildren();
    }
```
相关调用方法
```java
    //mDirtyHierarchy的值只有在requestLayout方法下被更新
    public void requestLayout() {
        super.requestLayout();
        mDirtyHierarchy = true;
    }

    //sortChildren()方法对横向纵向关系的view的数组进行非空判断 用DependencyGraph进行判断
    private void sortChildren() {
        final int count = getChildCount();
        if (mSortedVerticalChildren == null || mSortedVerticalChildren.length != count) {
            mSortedVerticalChildren = new View[count];
        }

        if (mSortedHorizontalChildren == null || mSortedHorizontalChildren.length != count) {
            mSortedHorizontalChildren = new View[count];
        }

        final DependencyGraph graph = mGraph;
        graph.clear();

        for (int i = 0; i < count; i++) {
            graph.add(getChildAt(i));
        }

        graph.getSortedViews(mSortedVerticalChildren, RULES_VERTICAL);
        graph.getSortedViews(mSortedHorizontalChildren, RULES_HORIZONTAL);
    }
```
**DependencyGraph的相关方法**
```java
    private static class DependencyGraph {
        ...
        /**
         * Adds a view to the graph.
         *
         * @param view The view to be added as a node to the graph.
         */
        void add(View view) {
            //因为是图 根据view生成一个节点
            final int id = view.getId();
            final Node node = Node.acquire(view);
            //如果是有效的id 则将该节点添加到List中
            if (id != View.NO_ID) {
                mKeyNodes.put(id, node);
            }

            mNodes.add(node);
        }     

          /**
           * Builds a sorted list of views. The sorting order depends on the dependencies
           * between the view. For instance, if view C needs view A to be processed first
           * and view A needs view B to be processed first, the dependency graph
           * is: B -> A -> C. The sorted array will contain views B, A and C in this order.
           *
           * @param sorted The sorted list of views. The length of this array must
           *        be equal to getChildCount().
           * @param rules The list of rules to take into account.
           */
          void getSortedViews(View[] sorted, int... rules) {
            //当前View找不到其它的可依赖的View时  作为root节点
              final ArrayDeque<Node> roots = findRoots(rules);
              int index = 0;

              Node node;
              //读取root的下一个node
              while ((node = roots.pollLast()) != null) {
                  final View view = node.view;
                  final int key = view.getId();

              //将符合规则的View加到 sorted中

                  sorted[index++] = view;

                  final ArrayMap<Node, DependencyGraph> dependents = node.dependents;
                  //dependents 依赖该node的node  (A C依赖B 则B的dependents中存A C)
                  final int count = dependents.size();
                  //遍历所有依赖自己的node
                  for (int i = 0; i < count; i++) {
                      final Node dependent = dependents.keyAt(i);
                      //dependencies 是被依赖的的node的规则和node(A 依赖 B D 则dependencies存有B D )
                      final SparseArray<Node> dependencies = dependent.dependencies;

                      //移除当前node和dependencies的依赖关系
                      dependencies.remove(key);
                      //如果解除依赖后没有其它依赖 则将该node也视为rootNode
                      if (dependencies.size() == 0) {
                          roots.add(dependent);
                      }
                  }
              }

              if (index < sorted.length) {
                  throw new IllegalStateException("Circular dependencies cannot exist in RelativeLayout");
              }
          }
  ...
}
```
eg: A依赖B B依赖C 首先存入C 因为不依赖任何其它的

```java

        /**
         * Finds the roots of the graph. A root is a node with no dependency and
         * with [0..n] dependents.
         *
         * @param rulesFilter The list of rules to consider when building the
         *        dependencies
         *
         * @return A list of node, each being a root of the graph
         */
        private ArrayDeque<Node> findRoots(int[] rulesFilter) {
          //keyNodes为nodelist
            final SparseArray<Node> keyNodes = mKeyNodes;
            final ArrayList<Node> nodes = mNodes;
            final int count = nodes.size();

          //初始化依赖该node的node和该node依赖的node相关参数
            for (int i = 0; i < count; i++) {
                final Node node = nodes.get(i);
                node.dependents.clear();
                node.dependencies.clear();
            }


            //遍历所有node  存入当前view和他所依赖的关系
            for (int i = 0; i < count; i++) {
                final Node node = nodes.get(i);

                final LayoutParams layoutParams = (LayoutParams) node.view.getLayoutParams();
                //取出当前View所有的依赖关系
                final int[] rules = layoutParams.mRules;
                final int rulesCount = rulesFilter.length;

                //遍历当前View所有的
                for (int j = 0; j < rulesCount; j++) {
                  //rule对应被依赖view的id
                    final int rule = rules[rulesFilter[j]];
                    if (rule > 0) {
                        //找到被依赖的node
                        final Node dependency = keyNodes.get(rule);
                        //跳过空view和本身
                        if (dependency == null || dependency == node) {
                            continue;
                        }
                        //添加依赖被依赖的node
                        dependency.dependents.put(node, this);
                        node.dependencies.put(rule, dependency);
                    }
                }
            }

            final ArrayDeque<Node> roots = mRoots;
            roots.clear();

            // 再次遍历  如果该node的依赖关系为0 即该view不依赖任何view 则视为rootView
            for (int i = 0; i < count; i++) {
                final Node node = nodes.get(i);
                if (node.dependencies.size() == 0) roots.addLast(node);
            }

            return roots;
        }
```

#### 2 初始化相关变量
```java
  int myWidth = -1;
  int myHeight = -1;

  int width = 0;
  int height = 0;

  final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
  final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
  final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
  final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

  // 如果不是UNSPECIFIED模式 则将widthSize赋值于myWidth
  if (widthMode != MeasureSpec.UNSPECIFIED) {
      myWidth = widthSize;
  }
  // 如果不是UNSPECIFIED模式 则将heightSize赋值于myHeight
  if (heightMode != MeasureSpec.UNSPECIFIED) {
      myHeight = heightSize;
  }
  //如果是EXACTLY模式 则将myWidth和myHeight记录
  if (widthMode == MeasureSpec.EXACTLY) {
      width = myWidth;
  }

  if (heightMode == MeasureSpec.EXACTLY) {
      height = myHeight;
  }

  View ignore = null;
  //判断是否为Start 和  top 确定左上角坐标
  int gravity = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
  final boolean horizontalGravity = gravity != Gravity.START && gravity != 0;
  gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
  final boolean verticalGravity = gravity != Gravity.TOP && gravity != 0;

  int left = Integer.MAX_VALUE;
  int top = Integer.MAX_VALUE;
  int right = Integer.MIN_VALUE;
  int bottom = Integer.MIN_VALUE;

  boolean offsetHorizontalAxis = false;
  boolean offsetVerticalAxis = false;
  // 记录ignore的view
  if ((horizontalGravity || verticalGravity) && mIgnoreGravity != View.NO_ID) {
      ignore = findViewById(mIgnoreGravity);
  }
  //宽度个高度是否为warp模式
  final boolean isWrapContentWidth = widthMode != MeasureSpec.EXACTLY;
  final boolean isWrapContentHeight = heightMode != MeasureSpec.EXACTLY;

  //在计算和分配的子View的坐标的时候 需要用到父VIew的尺寸 但是暂时无法拿到准确值(待完成下面操作)
  //先使用默认值代替 在计算后 用偏移量更新真是坐标
  final int layoutDirection = getLayoutDirection();
  if (isLayoutRtl() && myWidth == -1) {
      myWidth = DEFAULT_WIDTH;
  }
```

#### 3  遍历水平关系的View
```java
    View[] views = mSortedHorizontalChildren;
    int count = views.length;

    for (int i = 0; i < count; i++) {
        View child = views[i];
        if (child.getVisibility() != GONE) {
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            //根据方向获得子View中设置的规则
            int[] rules = params.getRules(layoutDirection);
            //将左右方向规则转换为左右的坐标
            applyHorizontalSizeRules(params, myWidth, rules);
            //测算水平方向的子View的尺寸
            measureChildHorizontal(child, params, myWidth, myHeight);
            //确定水平方向子View的位置
            if (positionChildHorizontal(child, params, myWidth, isWrapContentWidth)) {
                offsetHorizontalAxis = true;
            }
        }
    }

```
相关方法
```java
    private void applyHorizontalSizeRules(LayoutParams childParams, int myWidth, int[] rules) {
        RelativeLayout.LayoutParams anchorParams;
        childParams.mLeft = VALUE_NOT_SET;
        childParams.mRight = VALUE_NOT_SET;
        //得到当前子View的layout_toLeftOf属性对应的View
        anchorParams = getRelatedViewParams(rules, LEFT_OF);
        if (anchorParams != null) {
          //如果这个属性存在 则当前子View的右坐标是layout_toLeftOf对应的view的左坐标减去对应view的marginLeft的值和自身marginRight的值
            childParams.mRight = anchorParams.mLeft - (anchorParams.leftMargin +
                    childParams.rightMargin);
        //如果alignWithParent为true alignWithParent取alignWithParentIfMissing
        //如果layout_toLeftOf的view为空 或者gone 则将RelativeLayout当做被依赖的对象
        } else if (childParams.alignWithParent && rules[LEFT_OF] != 0) {
            //如果父容器RelativeLayout的宽度大于0
            //则子View的右坐标为 父RelativeLayout的宽度减去 mPaddingRight 和自身的marginRight
            if (myWidth >= 0) {
                childParams.mRight = myWidth - mPaddingRight - childParams.rightMargin;
            }
        }

        //类似的方法 得到左坐标(通过参数RIGHT_OF)
        anchorParams = getRelatedViewParams(rules, RIGHT_OF);
        if (anchorParams != null) {
            childParams.mLeft = anchorParams.mRight + (anchorParams.rightMargin +
                    childParams.leftMargin);
        } else if (childParams.alignWithParent && rules[RIGHT_OF] != 0) {
            childParams.mLeft = mPaddingLeft + childParams.leftMargin;
        }
        //类似的方法 得到左坐标 (通过参数ALIGN_LEFT)
        anchorParams = getRelatedViewParams(rules, ALIGN_LEFT);
        if (anchorParams != null) {
            childParams.mLeft = anchorParams.mLeft + childParams.leftMargin;
        } else if (childParams.alignWithParent && rules[ALIGN_LEFT] != 0) {
            childParams.mLeft = mPaddingLeft + childParams.leftMargin;
        }
        //类似的方法 得到右坐标 (通过参数ALIGN_RIGHT)
        anchorParams = getRelatedViewParams(rules, ALIGN_RIGHT);
        if (anchorParams != null) {
            childParams.mRight = anchorParams.mRight - childParams.rightMargin;
        } else if (childParams.alignWithParent && rules[ALIGN_RIGHT] != 0) {
            if (myWidth >= 0) {
                childParams.mRight = myWidth - mPaddingRight - childParams.rightMargin;
            }
        }
        //根据ALIGN_PARENT_LEFT 将自己放到父RelativeLayout的左边
        if (0 != rules[ALIGN_PARENT_LEFT]) {
            childParams.mLeft = mPaddingLeft + childParams.leftMargin;
        }
        //根据ALIGN_PARENT_RIGHT 将自己放到父RelativeLayout的右边
        if (0 != rules[ALIGN_PARENT_RIGHT]) {
            if (myWidth >= 0) {
                childParams.mRight = myWidth - mPaddingRight - childParams.rightMargin;
            }
        }
    }
```
```java
private void measureChildHorizontal(View child, LayoutParams params, int myWidth, int myHeight) {
  //获得child的宽度MeasureSpec
    final int childWidthMeasureSpec = getChildMeasureSpec(params.mLeft, params.mRight,
            params.width, params.leftMargin, params.rightMargin, mPaddingLeft, mPaddingRight,
            myWidth);

    final int childHeightMeasureSpec;
    //在低于4.2的时候 mAllowBrokenMeasureSpecs为true
    //当myHeight < 0 时 则根据父RelativeLayout设置其MeasureSpec模式
    if (myHeight < 0 && !mAllowBrokenMeasureSpecs) {
        //如果父RelativeLayout的height大于0  则 设置子view的MeasureSpec模式为EXACTLY
        if (params.height >= 0) {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    params.height, MeasureSpec.EXACTLY);
        } else {
            //反之 如果其小于0  则设置子View的MeasureSpec为UNSPECIFIED
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
    } else {
      //当当前myHeight >= 0
      //判断当前高度是否与父RelativeLayout高度相同 设置heightMode
      //根据maxHeight 和heightMode设置子View的MeasureSpec模式
        final int maxHeight;
        if (mMeasureVerticalWithPaddingMargin) {
            maxHeight = Math.max(0, myHeight - mPaddingTop - mPaddingBottom
                    - params.topMargin - params.bottomMargin);
        } else {
            maxHeight = Math.max(0, myHeight);
        }
        final int heightMode;
        if (params.height == LayoutParams.MATCH_PARENT) {
            heightMode = MeasureSpec.EXACTLY;
        } else {
            heightMode = MeasureSpec.AT_MOST;
        }
        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, heightMode);
    }
    //获得了子View的WidthMeasureSpec和HeightMeasureSpec
    //子View可以通过measure方法获取自身的size
    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
}
```
```java
/**
    * Get a measure spec that accounts for all of the constraints on this view.
    * This includes size constraints imposed by the RelativeLayout as well as
    * the View's desired dimension.
    *
    * @param childStart The left or top field of the child's layout params
    * @param childEnd The right or bottom field of the child's layout params
    * @param childSize The child's desired size (the width or height field of
    *        the child's layout params)
    * @param startMargin The left or top margin
    * @param endMargin The right or bottom margin
    * @param startPadding mPaddingLeft or mPaddingTop
    * @param endPadding mPaddingRight or mPaddingBottom
    * @param mySize The width or height of this view (the RelativeLayout)
    * @return MeasureSpec for the child
    */
   private int getChildMeasureSpec(int childStart, int childEnd,
           int childSize, int startMargin, int endMargin, int startPadding,
           int endPadding, int mySize) {
       int childSpecMode = 0;
       int childSpecSize = 0;

       final boolean isUnspecified = mySize < 0;
       //如果父RelativeLayout宽度小于0 版本号不小于4.2
       if (isUnspecified && !mAllowBrokenMeasureSpecs) {
            //如果子View的左边距和右边距都不为VALUE_NOT_SET
            //且右边距坐标大于左边距坐标 则将其差当做宽度赋予View 设置模式为EXACTLY
            //VALUE_NOT_SET = Integer.MIN_VALUE
            /**
             * Constant for the minimum {@code int} value, -2<sup>31</sup>.
             */
            //public static final int MIN_VALUE = 0x80000000;
           if (childStart != VALUE_NOT_SET && childEnd != VALUE_NOT_SET) {
               childSpecSize = Math.max(0, childEnd - childStart);
               childSpecMode = MeasureSpec.EXACTLY;
           } else if (childSize >= 0) {
               // 如果childSpecSize >= 0 则赋值于childSpecSize
               //同样设置模式为EXACTLY
               childSpecSize = childSize;
               childSpecMode = MeasureSpec.EXACTLY;
           } else {
               // 都不满足则设置模式为UNSPECIFIED
               childSpecSize = 0;
               childSpecMode = MeasureSpec.UNSPECIFIED;
           }

           return MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode);
       }

       // 计算 开始和结束相关
       int tempStart = childStart;
       int tempEnd = childEnd;

       //如果没有指定start值 则默认赋予 padding和merage的值
       if (tempStart == VALUE_NOT_SET) {
           tempStart = startPadding + startMargin;
       }
       //同上
       if (tempEnd == VALUE_NOT_SET) {
           tempEnd = mySize - endPadding - endMargin;
       }

       //指定最大可提供的大小
       final int maxAvailable = tempEnd - tempStart;

       if (childStart != VALUE_NOT_SET && childEnd != VALUE_NOT_SET) {
           //如果Start和End都是有效值 根据isUnspecified设置specMode为UNSPECIFIED或EXACTLY
           //并将设置对应的size
           childSpecMode = isUnspecified ? MeasureSpec.UNSPECIFIED : MeasureSpec.EXACTLY;
           childSpecSize = Math.max(0, maxAvailable);
       } else {
          //反之 判断childSize的相关值
           if (childSize >= 0) {
               //设置模式为EXACTLY
               //判断maxAvailable和childSize情况 取较大值设置为childSpecSize
               childSpecMode = MeasureSpec.EXACTLY;

               if (maxAvailable >= 0) {
                   // We have a maximum size in this dimension.
                   childSpecSize = Math.min(maxAvailable, childSize);
               } else {
                   // We can grow in this dimension.
                   childSpecSize = childSize;
               }
           } else if (childSize == LayoutParams.MATCH_PARENT) {
               //如果子View是match模式 参照isUnspecified设置相关
               childSpecMode = isUnspecified ? MeasureSpec.UNSPECIFIED : MeasureSpec.EXACTLY;
               childSpecSize = Math.max(0, maxAvailable);
           } else if (childSize == LayoutParams.WRAP_CONTENT) {
               //在wrap进行设置
               if (maxAvailable >= 0) {
                   // We have a maximum size in this dimension.
                   childSpecMode = MeasureSpec.AT_MOST;
                   childSpecSize = maxAvailable;
               } else {
                   // We can grow in this dimension. Child can be as big as it
                   // wants.
                   childSpecMode = MeasureSpec.UNSPECIFIED;
                   childSpecSize = 0;
               }
           }
       }

       return MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode);
   }
```
以上 完成了View的第一次测量  确定了View的大小 然后根据大小觉得把子view放在父RelativeLayout中的位置

```java
private boolean positionChildHorizontal(View child, LayoutParams params, int myWidth,
        boolean wrapContent) {
    //获取RelativeLayout的布局方向
    final int layoutDirection = getLayoutDirection();
    int[] rules = params.getRules(layoutDirection);

    if (params.mLeft == VALUE_NOT_SET && params.mRight != VALUE_NOT_SET) {
        // 如果右边界有效 左边界无效 根据右边界计算出左边界
        params.mLeft = params.mRight - child.getMeasuredWidth();
    } else if (params.mLeft != VALUE_NOT_SET && params.mRight == VALUE_NOT_SET) {
        // 同上反之
        params.mRight = params.mLeft + child.getMeasuredWidth();
    } else if (params.mLeft == VALUE_NOT_SET && params.mRight == VALUE_NOT_SET) {
        //都无效的时候

        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_HORIZONTAL] != 0) {
          //设置了CENTER_IN_PARENT或者 CENTER_HORIZONTAL的情况下
            if (!wrapContent) {
              //非wrap情况下
              //把子View水平中心固定在RelativeLayout的中心
                centerHorizontal(child, params, myWidth);
            } else {
               //左边距为padding+margin
               //右边距为左边距加上测量宽度
                params.mLeft = mPaddingLeft + params.leftMargin;
                params.mRight = params.mLeft + child.getMeasuredWidth();
            }
            return true;
        } else {
            //RTL右到左 布局方向
            //LTR左到右 布局方向
            if (isLayoutRtl()) {
                params.mRight = myWidth - mPaddingRight- params.rightMargin;
                params.mLeft = params.mRight - child.getMeasuredWidth();
            } else {
                params.mLeft = mPaddingLeft + params.leftMargin;
                params.mRight = params.mLeft + child.getMeasuredWidth();
            }
        }
    }
    return rules[ALIGN_PARENT_END] != 0;
    //当为CENTER_IN_PARENT  CENTER_HORIZONTAL ALIGN_PARENT_END三种情况之一时返回True
}
```

#### 4 遍历竖直关系的View
```java
...
  for (int i = 0; i < count; i++) {
           final View child = views[i];
           if (child.getVisibility() != GONE) {
               final LayoutParams params = (LayoutParams) child.getLayoutParams();
              //将竖直方向规则转换为坐标
               applyVerticalSizeRules(params, myHeight, child.getBaseline());
               //测量子View
               measureChild(child, params, myWidth, myHeight);
               //确定竖直方向子View的位置
               if (positionChildVertical(child, params, myHeight, isWrapContentHeight)) {
                   offsetVerticalAxis = true;
               }
              //首先判断是否为wrap模式
               if (isWrapContentWidth) {
                 //根据RTL或者LTR和版本进行区分
                 //Build.VERSION_CODES.KITKAT = 19
                 //主要对margin进行处理
                   if (isLayoutRtl()) {
                       if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
                           width = Math.max(width, myWidth - params.mLeft);
                       } else {
                           width = Math.max(width, myWidth - params.mLeft - params.leftMargin);
                       }
                   } else {
                       if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
                           width = Math.max(width, params.mRight);
                       } else {
                           width = Math.max(width, params.mRight + params.rightMargin);
                       }
                   }
               }
               if (isWrapContentHeight) {
                   if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
                       height = Math.max(height, params.mBottom);
                   } else {
                       height = Math.max(height, params.mBottom + params.bottomMargin);
                   }
               }

               if (child != ignore || verticalGravity) {
                   left = Math.min(left, params.mLeft - params.leftMargin);
                   top = Math.min(top, params.mTop - params.topMargin);
               }

               if (child != ignore || horizontalGravity) {
                   right = Math.max(right, params.mRight + params.rightMargin);
                   bottom = Math.max(bottom, params.mBottom + params.bottomMargin);
               }
           }
       }
...
```

#### 5 baseline计算
```java
// Use the top-start-most laid out view as the baseline. RTL offsets are
// applied later, so we can use the left-most edge as the starting edge.
    View baselineView = null;
    LayoutParams baselineParams = null;
    for (int i = 0; i < count; i++) {
        final View child = views[i];
        if (child.getVisibility() != GONE) {
            final LayoutParams childParams = (LayoutParams) child.getLayoutParams();
            if (baselineView == null || baselineParams == null
                    || compareLayoutPosition(childParams, baselineParams) < 0) {
                baselineView = child;
                baselineParams = childParams;
            }
        }
    }
    mBaselineView = baselineView;
```

#### 6 宽度和高度修正
```java
    //如何是wrap模式
    if (isWrapContentWidth) {
            width += mPaddingRight;

            if (mLayoutParams != null && mLayoutParams.width >= 0) {
                width = Math.max(width, mLayoutParams.width);
            }

            width = Math.max(width, getSuggestedMinimumWidth());
            width = resolveSize(width, widthMeasureSpec);

            //在得到最后的width之后 对依赖RelativeLayout的子View添上偏移量
            if (offsetHorizontalAxis) {
                for (int i = 0; i < count; i++) {
                    final View child = views[i];
                    if (child.getVisibility() != GONE) {
                        final LayoutParams params = (LayoutParams) child.getLayoutParams();
                        final int[] rules = params.getRules(layoutDirection);
                        //对CENTER_IN_PARENT或者CENTER_HORIZONTAL的子View重测
                        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_HORIZONTAL] != 0) {
                            centerHorizontal(child, params, width);
                        //对ALIGN_PARENT_RIGHT重测
                        } else if (rules[ALIGN_PARENT_RIGHT] != 0) {
                            final int childWidth = child.getMeasuredWidth();
                            params.mLeft = width - mPaddingRight - childWidth;
                            params.mRight = params.mLeft + childWidth;
                        }
                    }
                }
            }
        }
        //同上
        if (isWrapContentHeight) {
            height += mPaddingBottom;

            if (mLayoutParams != null && mLayoutParams.height >= 0) {
                height = Math.max(height, mLayoutParams.height);
            }

            height = Math.max(height, getSuggestedMinimumHeight());
            height = resolveSize(height, heightMeasureSpec);

            if (offsetVerticalAxis) {
                for (int i = 0; i < count; i++) {
                    final View child = views[i];
                    if (child.getVisibility() != GONE) {
                        final LayoutParams params = (LayoutParams) child.getLayoutParams();
                        final int[] rules = params.getRules(layoutDirection);
                        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_VERTICAL] != 0) {
                            centerVertical(child, params, height);
                        } else if (rules[ALIGN_PARENT_BOTTOM] != 0) {
                            final int childHeight = child.getMeasuredHeight();
                            params.mTop = height - mPaddingBottom - childHeight;
                            params.mBottom = params.mTop + childHeight;
                        }
                    }
                }
            }
        }


        //根据gravity再次修正
        if (horizontalGravity || verticalGravity) {
            final Rect selfBounds = mSelfBounds;
            selfBounds.set(mPaddingLeft, mPaddingTop, width - mPaddingRight,
                    height - mPaddingBottom);

            final Rect contentBounds = mContentBounds;
            Gravity.apply(mGravity, right - left, bottom - top, selfBounds, contentBounds,
                    layoutDirection);

            final int horizontalOffset = contentBounds.left - left;
            final int verticalOffset = contentBounds.top - top;
            if (horizontalOffset != 0 || verticalOffset != 0) {
                for (int i = 0; i < count; i++) {
                    final View child = views[i];
                    if (child.getVisibility() != GONE && child != ignore) {
                        final LayoutParams params = (LayoutParams) child.getLayoutParams();
                        if (horizontalGravity) {
                            params.mLeft += horizontalOffset;
                            params.mRight += horizontalOffset;
                        }
                        if (verticalGravity) {
                            params.mTop += verticalOffset;
                            params.mBottom += verticalOffset;
                        }
                    }
                }
            }
        }

        //如果是RTL(右到左显示)则再次修改
        if (isLayoutRtl()) {
            final int offsetWidth = myWidth - width;
            for (int i = 0; i < count; i++) {
                final View child = views[i];
                if (child.getVisibility() != GONE) {
                    final LayoutParams params = (LayoutParams) child.getLayoutParams();
                    params.mLeft -= offsetWidth;
                    params.mRight -= offsetWidth;
                }
            }
        }
```
#### 简单总结
RelativeLayout更加关注子View的left right top bottom值 并且优先级高于width和height



### RelativeLayout的layout过程
对于RelativeLayout来的 layout过程更多的根据子View的left right top bottom值来设定位置
```java
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
```

### RelativeLayout的draw过程
RelativeLayout作为ViewGroup的子类 因为其性质原因  没有对draw过程进行修改


以上
