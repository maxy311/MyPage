package com.wutian.maxy.nested.rvr.widget;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;

import com.wutian.maxy.nested.R;
import com.wutian.maxy.nested.widget.VelocityRecyclerView;

import java.lang.reflect.Field;

public class ParentRecyclerView extends VelocityRecyclerView implements NestedScrollingParent3 {
    private View childParentContainer;
    private ViewPager innerViewPager;
    private boolean doNotInterceptTouchEvent;
    private boolean innerIsStickyTop;
    private int stickyHeight = 0;

    public ParentRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public ParentRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, -1);
    }

    public ParentRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public String getTag() {
        return super.getTag() + "_Parent";
    }

    @Override
    protected void initView(Context context) {
        super.initView(context);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                adjustChildPagerContainerHeight();
            }
        });
    }

    public void setStickyHeight(int stickyHeight) {
        final int scrollOffset = this.stickyHeight - stickyHeight;
        this.stickyHeight = stickyHeight;
        adjustChildPagerContainerHeight();
        post(new Runnable() {
            @Override
            public void run() {
                scrollBy(0, scrollOffset);
            }
        });
    }

    public void setInnerViewPager(ViewPager innerViewPager) {
        this.innerViewPager = innerViewPager;
    }

    public void setChildParentContainer(View childParentContainer) {
        this.childParentContainer = childParentContainer;
        post(new Runnable() {
            @Override
            public void run() {
                adjustChildPagerContainerHeight();
            }
        });
    }

    public void adjustChildPagerContainerHeight() {
        if (childParentContainer == null || !ViewCompat.isAttachedToWindow(childParentContainer))
            return;
        ViewGroup.LayoutParams layoutParams = childParentContainer.getLayoutParams();
        int newHeight = getHeight() - stickyHeight;
        if (newHeight != layoutParams.height) {
            layoutParams.height = newHeight;
            childParentContainer.setLayoutParams(layoutParams);
        }

        if (innerIsStickyTop && childParentContainer.getTop() > 0) {
            scrollBy(0, childParentContainer.getTop());
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        if (state == SCROLL_STATE_IDLE) {
            int velocityY = (int) getVelocityY();
            if (velocityY > 0) {
                ChildRecyclerView currentChildRecyclerView = findCurrentChildRecyclerView();
                if (currentChildRecyclerView != null)
                    currentChildRecyclerView.fling(0, velocityY);
            }
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            ChildRecyclerView currentChildRecyclerView = findCurrentChildRecyclerView();
            //
            doNotInterceptTouchEvent = doNotInterceptTouch(e.getRawY(), currentChildRecyclerView);
            //2.
            stopFling();
            if (currentChildRecyclerView != null)
                currentChildRecyclerView.stopFling();
        }
        if (doNotInterceptTouchEvent)
            return false;
        else
            return super.onInterceptTouchEvent(e);
    }

    private ChildRecyclerView findCurrentChildRecyclerView() {
        try {
            if (innerViewPager != null) {
                int currentItem = innerViewPager.getCurrentItem();
                for (int i = 0; i < innerViewPager.getChildCount(); i++) {
                    View childAt = innerViewPager.getChildAt(i);
                    ViewPager.LayoutParams layoutParams = (ViewPager.LayoutParams) childAt.getLayoutParams();
                    if (layoutParams.isDecor)
                        continue;
                    Field positionField = layoutParams.getClass().getDeclaredField("position");
                    positionField.setAccessible(true);
                    int position = (int) positionField.get(layoutParams);
                    if (currentItem == position) {
                        if (childAt instanceof ChildRecyclerView)
                            return ((ChildRecyclerView) childAt);
                        else {
                            Object childAtTag = childAt.getTag(R.id.tag_saved_child_recycler_view);
                            if (childAtTag instanceof ChildRecyclerView)
                                return ((ChildRecyclerView) childAtTag);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private boolean doNotInterceptTouch(float rawY, ChildRecyclerView childRecyclerView) {
        if (childRecyclerView == null || childParentContainer == null || !ViewCompat.isAttachedToWindow(childRecyclerView))
            return false;
        int[] childLocation = new int[2];
        childRecyclerView.getLocationOnScreen(childLocation);
        int childRecyclerViewY = childLocation[1];
        if (rawY > childRecyclerViewY)
            return true;
        if (childParentContainer.getTop() == stickyHeight)
            return true;
        return false;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        boolean currentStickTop = false;
        if (childParentContainer != null && childParentContainer.getTop() == stickyHeight) {
            currentStickTop = true;
        }

        if (currentStickTop != innerIsStickyTop) {
            innerIsStickyTop = currentStickTop;
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {

    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return target instanceof ChildRecyclerView;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {

    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {

    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {

    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (target instanceof ChildRecyclerView) {
            int consumeY = dy;
            ChildRecyclerView childRecyclerView = (ChildRecyclerView) target;
            int childScrollY = childRecyclerView.getVerticalScrollY();
            if (childParentContainer.getTop() > stickyHeight) {
                if (childScrollY > 0 && dy < 0) {
                    consumeY = 0;
                } else if (childParentContainer.getTop() - dy < stickyHeight) {
                    consumeY = childParentContainer.getTop() - stickyHeight;
                }
            } else if (childParentContainer.getTop() == stickyHeight) {
                if (-dy < childScrollY)
                    consumeY = 0;
                else
                    consumeY = dy + childScrollY;
            }

            if (consumeY != 0) {
                consumed[1] = consumeY;
                this.scrollBy(0, consumeY);
            }
        }
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (Build.VERSION.SDK_INT > 19)
            return super.onNestedPreFling(target, velocityX, velocityY);
        return false;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        if (Build.VERSION.SDK_INT > 19)
            return super.onNestedFling(target, velocityX, velocityY, consumed);
        return true;
    }

    @Override
    public int getNestedScrollAxes() {
        return ViewCompat.SCROLL_AXIS_VERTICAL;
    }
}
