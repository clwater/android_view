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

   ​









