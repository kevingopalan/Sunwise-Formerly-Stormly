package com.venomdevelopment.sunwise;

import android.content.Context;
import androidx.annotation.FontRes;

/**
 * GraphViewUtils is retained as a no-op compatibility shim after removing GraphView.
 * Methods here are intentionally empty so existing static imports don't break builds.
 */
public final class GraphViewUtils {
    private GraphViewUtils() {}

    public static void setLabelTypeface(Context context, Object graphViewLike, @FontRes int fontResId) {
        // No-op
    }

    public static void setTitleTypeface(Context context, Object graphViewLike, @FontRes int fontResId) {
        // No-op
    }
}