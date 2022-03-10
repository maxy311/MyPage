package com.wutian.maxy.nested.rvr.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.wutian.maxy.nested.R;
import com.wutian.maxy.nested.widget.VelocityRecyclerView;


public class ChildRecyclerView extends VelocityRecyclerView {
    private static final int DRAG_IDLE = 0;
    private static final int DRAG_VERTICAL = 1;
    private static final int DRAG_HORIZONTAL = 2;

    private ParentRecyclerView parentRecyclerView;
    private int mTouchSlop;
    private float downX;
    private float downY;
    private int dragState = DRAG_IDLE;

    public ChildRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public ChildRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ChildRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public String getTag() {
        return super.getTag() + "_Child";
    }

    @Override
    protected void initView(Context context) {
        super.initView(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        connectToParent();
    }

    private void connectToParent() {
        View lastTraverseView = null;
        ViewPager viewPager = null;
        ViewGroup parentView = (ViewGroup) getParent();
        int parentCount = 0;
        while (parentView != null) {
            if (parentView instanceof ViewPager) {
                // 使用ViewPager，parentView顺序如下：
                // ChildRecyclerView -> 若干View -> ViewPager -> 若干View -> ParentRecyclerView
                // 此处将ChildRecyclerView保存到ViewPager最直接的子View中
                if (lastTraverseView != null)
                    lastTraverseView.setTag(R.id.tag_saved_child_recycler_view, this);
                viewPager = (ViewPager) parentView;
            } else if (parentView instanceof ParentRecyclerView) {
                parentRecyclerView = (ParentRecyclerView) parentView;
                parentRecyclerView.setInnerViewPager(viewPager);
                parentRecyclerView.setChildParentContainer(lastTraverseView);
                return;
            }
            parentCount++;
            if (parentCount > 3) {
                break;
            }
            lastTraverseView = parentView;
            if (parentView.getParent() instanceof ViewGroup) {
                parentView = (ViewGroup) parentView.getParent();
            }
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (parentRecyclerView != null) {
            if (state == SCROLL_STATE_IDLE) {
                int velocityY = (int) getVelocityY();
                if (velocityY < 0 && getVerticalScrollY() == 0) {
                    parentRecyclerView.fling(0, velocityY);
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            dragState = DRAG_IDLE;
            downX = e.getRawX();
            downY = e.getRawY();
            stopFling();

            getParent().requestDisallowInterceptTouchEvent(true);
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
            formDragState(e);
            if (dragState == DRAG_VERTICAL)
                return true;
        }
        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (MotionEvent.ACTION_MOVE == e.getAction()) {
            formDragState(e);
        }
        return super.onTouchEvent(e);
    }

    private void formDragState(MotionEvent e) {
        if (dragState == DRAG_IDLE) {
            float xDistance = Math.abs(e.getRawX() - downX);
            float yDistance = Math.abs(e.getRawY() - downY);
            if (xDistance > yDistance && xDistance > mTouchSlop) {
                dragState = DRAG_HORIZONTAL;
                getParent().requestDisallowInterceptTouchEvent(false);
            } else if (yDistance < xDistance && yDistance > mTouchSlop) {
                dragState = DRAG_VERTICAL;
            }
        }
    }

    public int getVerticalScrollY() {
        RecyclerView.ViewHolder firstHolder = this.findViewHolderForAdapterPosition(0);
        if (firstHolder != null && firstHolder.itemView.getTop() == 0)
            return 0;
        return this.computeVerticalScrollOffset();
    }
}
