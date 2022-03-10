package com.wutian.maxy.nested.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;

public class VelocityRecyclerView extends RecyclerView {
    private OverScroller overScroller;
    private Object scrollerYObj;
    private Field velocityYField;

    public VelocityRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public VelocityRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public VelocityRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        initView(context);
    }

    protected void initView(Context context) {
    }

    private void init(Context context) {
        try {
            // 1. mViewFlinger对象获取
            Field viewFlingField = RecyclerView.class.getDeclaredField("mViewFlinger");
            viewFlingField.setAccessible(true);
            Object viewFlingObj = viewFlingField.get(this);

            // 2. overScroller对象获取
            Field overScrollerFiled = viewFlingObj.getClass().getDeclaredField("mOverScroller");
            overScrollerFiled.setAccessible(true);
            overScroller = (OverScroller) overScrollerFiled.get(viewFlingObj);


            // 3. scrollerY对象获取  SplineOverScroller
            Field scrollerYField = OverScroller.class.getDeclaredField("mScrollerY");
            scrollerYField.setAccessible(true);
            scrollerYObj = scrollerYField.get(overScroller);

            // 4. Y轴速率filed获取  mCurrVelocity
            velocityYField = scrollerYObj.getClass().getDeclaredField("mCurrVelocity");
            velocityYField.setAccessible(true);
        } catch (Exception e) {
            Log.d(getTag(), "init Error : " + e.toString());
        }
    }

    /**
     * 获取垂直方向的速率
     */
    public float getVelocityY() {
        try {
            Log.d(getTag(), "getVelocityY ");
            return (float) velocityYField.get(scrollerYObj);
        } catch (Exception e) {
            Log.d(getTag(), "getVelocityY Error : " + e.toString());
        }
        return 0;
    }


    /**
     * 停止滑动fling
     */
    public void stopFling() {
        try {
            Log.d(getTag(), "stopFling ");
            overScroller.forceFinished(true);
            velocityYField.set(scrollerYObj, 0);
        } catch (Exception e) {
            Log.d(getTag(), "stopFling Error:  " + e.toString());
        }
    }

    public String getTag() {
        return "CustomRecyclerView_";
    }
}
