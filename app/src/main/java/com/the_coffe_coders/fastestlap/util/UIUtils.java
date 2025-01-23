package com.the_coffe_coders.fastestlap.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class UIUtils {

    public static void hideSystemUI(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    }

    public static void showSystemUI(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    }

    public static void applyWindowInsets(MaterialToolbar toolbar) {
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = systemBars.top;
            v.setLayoutParams(params);

            return insets;
        });
    }
    // SYSTEM_UI_FLAG_FULLSCREEN // Hide the status bar
    // SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // Let the layout expand into status bar
    // SYSTEM_UI_FLAG_LAYOUT_STABLE // avoid abrupt layout changes during toggling of status and navigation bars
    // SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hide the navigation bar
    // SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION; // Let the layout expand into navigation bar
}