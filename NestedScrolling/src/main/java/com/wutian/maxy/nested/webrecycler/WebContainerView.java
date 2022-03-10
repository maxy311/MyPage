package com.wutian.maxy.nested.webrecycler;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wutian.maxy.nested.utils.DensityUtils;

public class WebContainerView extends FrameLayout {
    private OnWebViewDataListener mWebLoadListener;

    protected WebView mWebView;
    private int mWebContentHeight;
    private boolean isWebDataLoaded;
    public WebContainerView(@NonNull Context context) {
        super(context, null);
    }

    public WebContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, -1);
    }

    public WebContainerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWebView(context);
    }

    private void initWebView(Context context) {
        mWebView = new WebView(context);
        addView(mWebView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    }

    public void load(final String url) {
        mWebView.loadUrl(url);
    }

    public int getWebViewHeight() {
        return mWebView.getContentHeight();
    }

    public int getWebViewScrollY() {
        return mWebView.getScrollY();
    }

    public void setWebLoadListener(OnWebViewDataListener webLoadListener) {
        this.mWebLoadListener = webLoadListener;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycle();
    }

    public void recycle() {
    }


    public interface OnWebViewDataListener {
        void onWebLoadSuccess(String url);

        void onWebLoadFail(String url);
    }

    private void resetWebContainerHeight(int scrollHeight) {
        int contentHeight = (int) DensityUtils.dipToPix(scrollHeight);
        mWebContentHeight = contentHeight;
        int containerHeight = getHeight();
        Log.d("Hybrid", "resetWebContainerHeight   getContainerHeight " + containerHeight + "    " + contentHeight);
        if (contentHeight > containerHeight) {
            contentHeight = containerHeight;
        }

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = contentHeight;
        setLayoutParams(layoutParams);
    }

    protected int getCalculateWebContentHeight() {
        return mWebContentHeight;
    }

    public boolean isWebDataLoaded() {
        return isWebDataLoaded;
    }
}
