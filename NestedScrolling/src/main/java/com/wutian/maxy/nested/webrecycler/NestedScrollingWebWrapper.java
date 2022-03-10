package com.wutian.maxy.nested.webrecycler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;

public class NestedScrollingWebWrapper extends WebContainerView implements NestedScrollingChild2 {
    private boolean mIsSelfFling;
    private boolean mHasFling;

    private final int TOUCH_SLOP;
    private int mMaximumVelocity;
    private int mFirstY;
    private int mLastY;
    private int mMaxScrollY;
    private int mWebViewContentHeight;

    private final int[] mScrollConsumed = new int[2];

    private NestedScrollingChildHelper mChildHelper;
    private NestedScrollingParentContainer mParentView;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;

    public NestedScrollingWebWrapper(Context context) {
        this(context, null);
    }

    public NestedScrollingWebWrapper(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedScrollingWebWrapper(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        mScroller = new Scroller(getContext());
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        TOUCH_SLOP = configuration.getScaledTouchSlop();

        mWebView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                NestedScrollingWebWrapper.this.onTouchEvent(event);
                return false;
            }
        });
    }

    public int getWebViewContentHeight() {
        if (mWebViewContentHeight == 0) {
            mWebViewContentHeight = getCalculateWebContentHeight();
        }
//        Log.d("wwwwww", "getWebViewContentHeight    " + mWebViewContentHeight);
        return mWebViewContentHeight;
    }

    public boolean canScrollDown() {
        final int range = getWebViewContentHeight() - getHeight();
        if (range <= 0) {
            return false;
        }

        final int offset = getWebViewScrollY();
        return offset < range - TOUCH_SLOP;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mWebViewContentHeight = 0;
                mLastY = (int) event.getRawY();
                mFirstY = mLastY;
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                initOrResetVelocityTracker();
                mIsSelfFling = false;
                mHasFling = false;
                mMaxScrollY = getWebViewContentHeight() - getHeight();
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(event);
                int y = (int) event.getRawY();
                int dy = y - mLastY;
                mLastY = y;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (!dispatchNestedPreScroll(0, -dy, mScrollConsumed, null)) {
                    scrollBy(0, -dy);
                }
                if (Math.abs(mFirstY - y) > TOUCH_SLOP) {
                    //屏蔽WebView本身的滑动，滑动事件自己处理
                    event.setAction(MotionEvent.ACTION_CANCEL);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isParentResetScroll() && mVelocityTracker != null) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int yVelocity = (int) -mVelocityTracker.getYVelocity();
                    recycleVelocityTracker();
                    mIsSelfFling = true;
                    flingScroll(0, yVelocity);
                }
                break;
        }
        super.onTouchEvent(event);
        return true;
    }

    public void flingScroll(int vx, int vy) {
        Log.d("wwwwwwwwww", "flingScroll     " + vy + "      " + getWebViewScrollY());
        mScroller.fling(0, getWebViewScrollY(), 0, vy, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycleVelocityTracker();
        stopScroll();
        mChildHelper = null;
        mScroller = null;
        mParentView = null;
        mWebView.setOnTouchListener(null);
    }

    @Override
    public void computeScroll() {
        if (mScroller == null)
            return;
        if (mScroller.computeScrollOffset()) {
            final int currY = mScroller.getCurrY();
            if (!mIsSelfFling) {
                // parent flying
                scrollTo(0, currY);
                invalidate();
                return;
            }

            if (isWebViewCanScroll()) {
                scrollTo(0, currY);
                invalidate();
            }
            if (!mHasFling
                    && mScroller.getStartY() < currY
                    && !canScrollDown()
                    && startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                    && !dispatchNestedPreFling(0, mScroller.getCurrVelocity())) {
                //滑动到底部时，将fling传递给父控件和RecyclerView
                mHasFling = true;
                dispatchNestedFling(0, mScroller.getCurrVelocity(), false);
            }
        }
    }

    @Override
    public void scrollBy(int x, int y) {
        mWebView.scrollBy(x, y);
    }

    @Override
    public void scrollTo(int x, int y) {
        if (y < 0) {
            y = 0;
        }
        if (mMaxScrollY != 0 && y > mMaxScrollY) {
            y = mMaxScrollY;
        }
        if (isParentResetScroll()) {
            // TODO: 12/4/20  
//            mWebView.scrollTo(x, y);
            mWebView.scrollTo(x, y);
        }
    }

    void scrollToBottom() {
        int y = getWebViewContentHeight();
        mWebView.scrollTo(0, y - getHeight());
    }

    private NestedScrollingChildHelper getNestedScrollingHelper() {
        if (mChildHelper == null) {
            mChildHelper = new NestedScrollingChildHelper(this);
        }
        return mChildHelper;
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void initWebViewParent() {
        if (this.mParentView != null) {
            return;
        }
        View parent = (View) getParent();
        while (parent != null) {
            if (parent instanceof NestedScrollingParentContainer) {
                this.mParentView = (NestedScrollingParentContainer) parent;
                break;
            } else {
                parent = (View) parent.getParent();
            }
        }
    }

    private boolean isParentResetScroll() {
        if (mParentView == null) {
            initWebViewParent();
        }
        if (mParentView != null) {
            return mParentView.getScrollY() == 0;
        }
        return true;
    }

    private void stopScroll() {
        if (mScroller != null && !mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
    }

    private boolean isWebViewCanScroll() {
        return getWebViewContentHeight() > getHeight();
    }

    /****** NestedScrollingChild BEGIN ******/
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        Log.d("wwwwwwww", "setNestedScrollingEnabled  " + enabled);
        getNestedScrollingHelper().setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        Log.d("wwwwwwww", "isNestedScrollingEnabled  ");
        return getNestedScrollingHelper().isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        Log.d("wwwwwwww", "startNestedScroll  ");
        return getNestedScrollingHelper().startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        Log.d("wwwwwwww", "stopNestedScroll  ");
        getNestedScrollingHelper().stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        boolean hasNestedScrollingParent = getNestedScrollingHelper().hasNestedScrollingParent();
        Log.d("wwwwwwww", "hasNestedScrollingParent    " + hasNestedScrollingParent);
        return hasNestedScrollingParent;
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow) {
        boolean dispatchNestedPreScroll = getNestedScrollingHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
        Log.d("wwwwwwww", "dispatchNestedPreScroll    " + dispatchNestedPreScroll);
        return dispatchNestedPreScroll;
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow) {
        boolean dispatchNestedScroll = getNestedScrollingHelper().dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
        Log.d("wwwwwwww", "dispatchNestedPreScroll    " + dispatchNestedScroll);
        return dispatchNestedScroll;
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        boolean dispatchNestedFling = getNestedScrollingHelper().dispatchNestedFling(velocityX, velocityY, consumed);
        Log.d("wwwwwwww", "dispatchNestedFling    " + dispatchNestedFling);
        return dispatchNestedFling;
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        boolean dispatchNestedPreFling = getNestedScrollingHelper().dispatchNestedPreFling(velocityX, velocityY);
        Log.d("wwwwwwww", "dispatchNestedPreFling    " + dispatchNestedPreFling);
        return dispatchNestedPreFling;
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        boolean startNestedScroll = getNestedScrollingHelper().startNestedScroll(axes, type);
        Log.d("wwwwwwww", "startNestedScroll    " + startNestedScroll);
        return startNestedScroll;
    }

    @Override
    public void stopNestedScroll(int type) {
        Log.d("wwwwwwww", "stopNestedScroll    " + type);
        getNestedScrollingHelper().stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        boolean hasNestedScrollingParent = getNestedScrollingHelper().hasNestedScrollingParent(type);
        Log.d("wwwwwwww", "hasNestedScrollingParent    " + type);
        return hasNestedScrollingParent;
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        boolean dispatchNestedScroll = getNestedScrollingHelper().dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
        Log.d("wwwwwwww", "dispatchNestedScroll    " + dispatchNestedScroll);
        return dispatchNestedScroll;
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        boolean dispatchNestedPreScroll = getNestedScrollingHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
        Log.d("wwwwwwww", "dispatchNestedScroll    " + dispatchNestedPreScroll);
        return dispatchNestedPreScroll;
    }

    public boolean hasScrolledContent() {
        return mWebView.getScrollY() != 0;
    }
}
