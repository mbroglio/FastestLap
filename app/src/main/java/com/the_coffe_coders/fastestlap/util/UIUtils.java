package com.the_coffe_coders.fastestlap.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.material.appbar.MaterialToolbar;
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
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
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

    public static void loadImageWithGlide(Context context, String url, ImageView imageView, Runnable onSuccess) {
        loadImage(context, url, imageView, onSuccess, 0);
    }

    public static void loadSequenceOfImagesWithGlide(Context context, String[] urls, ImageView[] imageViews, Runnable onSuccess) {
        if (urls.length != imageViews.length) {
            throw new IllegalArgumentException("The length of urls and imageViews must be the same");
        }

        for (int i = 0; i < urls.length; i++) {
            if (i == urls.length - 1) {
                loadImage(context, urls[i], imageViews[i], onSuccess, 0);
            } else {
                loadImage(context, urls[i], imageViews[i], null, 0);
            }
        }
    }

    private static void loadImage(Context context, String url, ImageView imageView, Runnable onSuccess, int retryCount) {
        Glide.with(context)
                .load(url)
                .into(new CustomTarget<Drawable>() {

                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Log.i("Glide", "Image loaded successfully: ");
                        imageView.setImageDrawable(resource);
                        if(onSuccess != null){
                            onSuccess.run();
                        }

                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle the case when the load is cleared
                        Log.i("Glide", "Image load cleared: " + url);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Log.e("Glide", "Image loading failed: " + url);
                        if (retryCount < Constants.MAX_RETRY_COUNT) {
                            Log.i("Glide", "Retrying image load: " + url + " - retry count: " + retryCount);
                            new Handler(Looper.getMainLooper()).post(() -> loadImage(context, url, imageView,  onSuccess, retryCount + 1));
                        }
                    }
                });
    }

    public static void singleSetTextViewText(String text, TextView textView) {
        setTextViewText(text, textView);
    }

    public static void multipleSetTextViewText(String[] texts, TextView[] textViews) {
        if(texts.length != textViews.length) {
            throw new IllegalArgumentException("The length of texts and textViews must be the same");
        }

        for (int i = 0; i < texts.length; i++) {
            setTextViewText(texts[i], textViews[i]);
        }
    }

    private static void setTextViewText(String text, TextView textView) {
        textView.setText(text);
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