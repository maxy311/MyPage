package com.wutian.maxy.nested.utils;

import android.content.res.Resources;
import android.util.TypedValue;

public class DensityUtils {
    public DensityUtils() {
    }

    public static float getDensity() {
        return Resources.getSystem().getDisplayMetrics().density;
    }

    public static int dip2px(float dip) {
        float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int)(dip * scale + 0.5F);
    }

    public static int px2dip(float pix) {
        float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int)(pix / scale + 0.5F);
    }

    public static float dipToPix(float dip) {
        return TypedValue.applyDimension(1, dip, Resources.getSystem().getDisplayMetrics());
    }

    public static float spToPix(float sp) {
        return TypedValue.applyDimension(2, sp, Resources.getSystem().getDisplayMetrics());
    }

    public static int pixToSp(float pix) {
        float fontScale = Resources.getSystem().getDisplayMetrics().scaledDensity;
        return (int)(pix / fontScale + 0.5F);
    }
}
