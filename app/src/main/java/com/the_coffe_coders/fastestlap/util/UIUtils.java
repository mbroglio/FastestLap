package com.the_coffe_coders.fastestlap.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

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
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = systemBars.top;
            v.setLayoutParams(params);

            return insets;
        });
    }

    public static void applyWindowInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = systemBars.bottom;
            v.setLayoutParams(params);

            return insets;
        });
    }

    public static GestureDetector createTapDetector(Activity activity) {
        return new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                boolean visible = (activity.getWindow().getDecorView().getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                if (visible) {
                    UIUtils.hideSystemUI(activity);
                } else {
                    UIUtils.showSystemUI(activity);
                }
                UIUtils.hideSystemUI(activity);
                return true;
            }
        });
    }

    public static void animateCardBackgroundColor(Context context, MaterialCardView cardView, int startColorResId, int endColor, int duration, int repeatCount) {
        int startColor = ContextCompat.getColor(context, startColorResId);

        ValueAnimator colorAnimator = ObjectAnimator.ofInt(cardView, "cardBackgroundColor", startColor, endColor);
        colorAnimator.setDuration(duration); // Duration in milliseconds
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setRepeatCount(repeatCount); // Repeat count
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cardView.setCardBackgroundColor(endColor);
            }
        });
        colorAnimator.start();
    }
    // SYSTEM_UI_FLAG_FULLSCREEN // Hide the status bar
    // SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // Let the layout expand into status bar
    // SYSTEM_UI_FLAG_LAYOUT_STABLE // avoid abrupt layout changes during toggling of status and navigation bars
    // SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hide the navigation bar
    // SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION; // Let the layout expand into navigation bar
}