## RelativeLayout  源码分析
> 继承自ViewGroup 没有重载onDraw方法 内部子View又是相对 只要计算出View的坐标 layout过程同样简单

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
           if (childSize >= 0) {
               // Child wanted an exact size. Give as much as possible.
               childSpecMode = MeasureSpec.EXACTLY;

               if (maxAvailable >= 0) {
                   // We have a maximum size in this dimension.
                   childSpecSize = Math.min(maxAvailable, childSize);
               } else {
                   // We can grow in this dimension.
                   childSpecSize = childSize;
               }
           } else if (childSize == LayoutParams.MATCH_PARENT) {
               // Child wanted to be as big as possible. Give all available
               // space.
               childSpecMode = isUnspecified ? MeasureSpec.UNSPECIFIED : MeasureSpec.EXACTLY;
               childSpecSize = Math.max(0, maxAvailable);
           } else if (childSize == LayoutParams.WRAP_CONTENT) {
               // Child wants to wrap content. Use AT_MOST to communicate
               // available space if we know our max size.
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
