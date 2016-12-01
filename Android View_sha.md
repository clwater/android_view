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
