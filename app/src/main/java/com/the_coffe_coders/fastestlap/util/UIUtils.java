package com.the_coffe_coders.fastestlap.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;

import java.security.MessageDigest;

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
                        if (retryCount <= Constants.MAX_RETRY_COUNT) {
                            Log.i("Glide", "Retrying image load: " + url + " - retry count: " + retryCount);
                            new Handler(Looper.getMainLooper()).post(() -> loadImage(context, url, imageView,  onSuccess, retryCount + 1));
                        }
                    }
                });
    }

    public static void loadImageInEventCardWithAlpha(Context context, String url, LinearLayout card, Runnable onSuccess, int alpha){
        loadImageAlpha(context, url, card, onSuccess, alpha, 0);
    }

    private static void loadImageAlpha(Context context, String url, LinearLayout card, Runnable onSuccess, int alpha, int retryCount) {

        Glide.with(context)
                .load(url)
                .transform(new BitmapTransformation() {
                    @Override
                    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {

                    }

                    @Override
                    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
                        // Make the bitmap 30% transparent (76/255 ≈ 0.3)
                        return setAlpha(toTransform, alpha);
                    }

                    public String getId() {
                        return "alpha";
                    }

                    // Helper method to set alpha on bitmap
                    private Bitmap setAlpha(Bitmap bitmap, int alpha) {
                        Bitmap mutableBitmap = bitmap.isMutable() ? bitmap : bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(mutableBitmap);
                        Paint paint = new Paint();
                        paint.setAlpha(alpha);
                        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
                        return mutableBitmap;
                    }
                })
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        card.setBackground(resource);
                        if(onSuccess != null){
                            onSuccess.run();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Use default image if loading fails
                        Drawable defaultImage = ContextCompat.getDrawable(context, R.drawable.constructors_image);
                        if (defaultImage != null) {
                            defaultImage.setAlpha(76);
                        }
                        card.setBackground(defaultImage);
                        if(onSuccess != null){
                            onSuccess.run();
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Log.e("Glide", "Image loading failed: " + url);
                        if (retryCount <= Constants.MAX_RETRY_COUNT) {
                            Log.i("Glide", "Retrying image load: " + url + " - retry count: " + retryCount);
                            new Handler(Looper.getMainLooper()).post(() -> loadImageAlpha(context, url, card, onSuccess, alpha, retryCount + 1));
                        }
                    }
                });
    }

    public static void singleSetTextViewText(String text, TextView textView) {
        setTextViewText(text, textView);
    }

    public static void setTextViewTextWithCondition(boolean condition, String textIfTrue, String textIfFalse, TextView textView) {
        if (condition) {
            setTextViewText(textIfTrue, textView);
        } else {
            setTextViewText(textIfFalse, textView);
        }
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

    public static void openLocation(Context context, String latitude, String longitude) {
        String uri = String.format(Constants.GOOGLE_MAPS_ACCESS, latitude, longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, R.string.no_map_app_found, Toast.LENGTH_SHORT).show();
            Log.e("UIUtils", "No map app found to open location");
        }
    }

    public static void openGoogleWeather(Context context, String locality) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(Constants.WEATHER_ACCESS_PACKAGE);
        if (intent != null) {
            intent.setAction(Intent.ACTION_SEARCH);
            intent.putExtra(SearchManager.QUERY, context.getString(R.string.weather, locality));
            context.startActivity(intent);
        } else {
            openWeatherInBrowser(context, locality);
        }
    }

    private static void openWeatherInBrowser(Context context, String locality) {
        String uri = String.format(Constants.GOOGLE_WEATHER_ACCESS, Uri.encode(locality));
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, context.getString(R.string.no_browser_found), Toast.LENGTH_SHORT).show();
        }
    }

    // SYSTEM_UI_FLAG_FULLSCREEN // Hide the status bar
    // SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // Let the layout expand into status bar
    // SYSTEM_UI_FLAG_LAYOUT_STABLE // avoid abrupt layout changes during toggling of status and navigation bars
    // SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hide the navigation bar
    // SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION; // Let the layout expand into navigation bar
}