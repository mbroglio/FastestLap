package com.the_coffe_coders.fastestlap.util.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.security.MessageDigest;

/**
 * Utility class dedicated to handling image loading, preloading, and caching with Glide.
 * Implements performance optimizations: DiskCacheStrategy.ALL, downsampling, and activity lifecycle guards.
 */
public class GlideUtils {

    private static final String TAG = "GlideUtils";

    /**
     * Helper to check if context belongs to an Activity that is destroyed or finishing.
     */
    private static boolean isActivityDestroyed(Context context) {
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            return activity.isDestroyed() || activity.isFinishing();
        }
        return false;
    }

    public static void loadImageWithGlide(Context context, String url, ImageView imageView, Runnable onSuccess) {
        loadImage(context, url, imageView, onSuccess, 0);
    }

    /**
     * Starts downloading the image at {@code url} into Glide's disk and memory cache.
     * Uses applicationContext to pin memory cache beyond Activity lifecycle.
     */
    public static void preloadImage(Context context, String url) {
        if (url == null || url.isEmpty()) return;
        if (context == null || isActivityDestroyed(context)) return;

        Glide.with(context.getApplicationContext())
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .downsample(DownsampleStrategy.AT_MOST)
                .preload();
    }

    public static void preloadImage(Context context, String url, Runnable onComplete) {
        if (url == null || url.isEmpty()) {
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
            return;
        }

        if (context == null || isActivityDestroyed(context)) {
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
            return;
        }

        Glide.with(context.getApplicationContext())
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .downsample(DownsampleStrategy.AT_MOST)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        if (onComplete != null) {
                            new Handler(Looper.getMainLooper()).post(onComplete);
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (onComplete != null) {
                            new Handler(Looper.getMainLooper()).post(onComplete);
                        }
                        return false;
                    }
                })
                .preload();
    }

    public static void preloadImagesInParallel(Context context, String[] urls, Runnable onSuccess) {
        if (urls == null || urls.length == 0) {
            if (onSuccess != null) {
                new Handler(Looper.getMainLooper()).post(onSuccess);
            }
            return;
        }

        final int[] loadedCount = {0};
        final int total = urls.length;

        Runnable check = () -> {
            synchronized (loadedCount) {
                loadedCount[0]++;
                if (loadedCount[0] >= total && onSuccess != null) {
                    new Handler(Looper.getMainLooper()).post(onSuccess);
                }
            }
        };

        for (String url : urls) {
            preloadImage(context, url, check);
        }
    }

    public static void loadSequenceOfImagesWithGlide(Context context, String[] urls, ImageView[] imageViews, Runnable onSuccess) {
        if (urls.length != imageViews.length) {
            throw new IllegalArgumentException("The length of urls and imageViews must be the same");
        }

        if (urls.length == 0) {
            if (onSuccess != null) {
                new Handler(Looper.getMainLooper()).post(onSuccess);
            }
            return;
        }

        loadImageSequentially(context, urls, imageViews, 0, onSuccess);
    }

    private static void loadImageSequentially(Context context, String[] urls, ImageView[] imageViews, int index, Runnable onSuccess) {
        if (index >= urls.length) {
            if (onSuccess != null) {
                new Handler(Looper.getMainLooper()).post(onSuccess);
            }
            return;
        }

        loadImage(context, urls[index], imageViews[index], () -> {
            loadImageSequentially(context, urls, imageViews, index + 1, onSuccess);
        }, 0);
    }

    public static void loadImagesInParallel(Context context, String[] urls, ImageView[] imageViews, Runnable onSuccess) {
        if (urls.length != imageViews.length) {
            throw new IllegalArgumentException("The length of urls and imageViews must be the same");
        }

        if (urls.length == 0) {
            if (onSuccess != null) {
                new Handler(Looper.getMainLooper()).post(onSuccess);
            }
            return;
        }

        final int[] loadedCount = {0};
        final int totalImages = urls.length;

        Runnable checkComplete = () -> {
            synchronized (loadedCount) {
                loadedCount[0]++;
                if (loadedCount[0] == totalImages && onSuccess != null) {
                    new Handler(Looper.getMainLooper()).post(onSuccess);
                }
            }
        };

        for (int i = 0; i < urls.length; i++) {
            loadImage(context, urls[i], imageViews[i], checkComplete, 0);
        }
    }

    public static void loadImageAsync(Context context, String url, ImageView imageView) {
        if (imageView == null) return;
        if (url == null || url.isEmpty()) return;
        if (context == null || isActivityDestroyed(context)) return;

        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .downsample(DownsampleStrategy.AT_MOST)
                .into(imageView);
    }

    private static void loadImage(Context context, String url, ImageView imageView, Runnable onSuccess, int retryCount) {
        Log.i(TAG, "Loading image: " + url);

        if (context == null || isActivityDestroyed(context)) {
            Log.w(TAG, "Skipping image load — activity is destroyed or context is null: " + url);
            if (onSuccess != null) {
                new Handler(Looper.getMainLooper()).post(onSuccess);
            }
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = cm != null ? cm.getActiveNetwork() : null;
        NetworkCapabilities nc = (activeNetwork != null) ? cm.getNetworkCapabilities(activeNetwork) : null;
        final boolean isConnected = nc != null
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        if (url != null && !url.isEmpty()) {
            final boolean[] callbackFired = {false};

            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .downsample(DownsampleStrategy.AT_MOST)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Image loading failed: " + url);

                            synchronized (callbackFired) {
                                if (callbackFired[0]) return true;
                                callbackFired[0] = true;
                            }

                            if (isConnected) {
                                if (retryCount <= Constants.MAX_RETRY_COUNT) {
                                    Log.i(TAG, "Retrying image load: " + url + " - retry count: " + retryCount);
                                    new Handler(Looper.getMainLooper()).post(() -> loadImage(context, url, imageView, onSuccess, retryCount + 1));
                                } else {
                                    Log.e(TAG, "Max retry count reached for image: " + url);
                                    manageContentLoadError(imageView, null, context, onSuccess, 0);
                                }
                            } else {
                                manageContentLoadError(imageView, null, context, onSuccess, 0);
                            }
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.i(TAG, "Full image loaded (firing callback): " + url);

                            synchronized (callbackFired) {
                                if (callbackFired[0])
                                    return false;
                                callbackFired[0] = true;
                            }

                            if (onSuccess != null) {
                                new Handler(Looper.getMainLooper()).post(onSuccess);
                            }
                            return false;
                        }
                    })
                    .into(imageView);
        } else {
            Log.e(TAG, "URL is null");
            manageContentLoadError(imageView, null, context, onSuccess, 0);
        }
    }

    public static void loadImageInEventCardWithAlpha(Context context, String url, LinearLayout card, Runnable onSuccess, int alpha) {
        loadImageAlpha(context, url, card, onSuccess, alpha, 0);
    }

    private static void loadImageAlpha(Context context, String url, LinearLayout card, Runnable onSuccess, int alpha, int retryCount) {
        if (context == null || isActivityDestroyed(context)) {
            Log.w(TAG, "Skipping alpha image load — activity is destroyed: " + url);
            if (onSuccess != null) {
                new Handler(Looper.getMainLooper()).post(onSuccess);
            }
            return;
        }

        if (url != null && !url.isEmpty()) {
            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .downsample(DownsampleStrategy.AT_MOST)
                    .transform(new BitmapTransformation() {
                        @Override
                        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
                        }

                        @Override
                        protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
                            return setAlpha(toTransform, alpha);
                        }

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
                            if (onSuccess != null) {
                                new Handler(Looper.getMainLooper()).post(onSuccess);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            Drawable defaultImage = ContextCompat.getDrawable(context, R.drawable.content_not_found_icon);
                            if (defaultImage != null) {
                                defaultImage.setAlpha(76);
                            }
                            card.setBackground(defaultImage);
                            if (onSuccess != null) {
                                new Handler(Looper.getMainLooper()).post(onSuccess);
                            }
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            Log.w(TAG, "Image loading failed: " + url);
                            if (retryCount <= Constants.MAX_RETRY_COUNT) {
                                Log.i(TAG, "Retrying image load: " + url + " - retry count: " + retryCount);
                                new Handler(Looper.getMainLooper()).post(() -> loadImageAlpha(context, url, card, onSuccess, alpha, retryCount + 1));
                            } else {
                                Log.e(TAG, "Max retry count reached for image: " + url);
                                manageContentLoadError(null, card, context, onSuccess, 1);
                            }
                        }
                    });
        } else {
            Log.e(TAG, "URL is null");
            manageContentLoadError(null, card, context, onSuccess, 1);
        }
    }

    private static void manageContentLoadError(ImageView imageView, LinearLayout layout, Context context, Runnable onSuccess, int contentType) {
        if (context != null) {
            switch (contentType) {
                case 0:
                    if (imageView != null) {
                        Log.e(TAG, "Image loading failed, setting backup image");
                        Drawable errorImage = AppCompatResources.getDrawable(context, R.drawable.content_not_found_icon);
                        imageView.setImageDrawable(errorImage);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    }
                    break;
                case 1:
                    if (layout != null) {
                        Log.e(TAG, "Layout loading failed, setting backup background");
                        layout.setBackgroundColor(context.getColor(R.color.timer_gray));
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown content type for error handling");
            }
        }

        if (onSuccess != null) {
            onSuccess.run();
        }
    }
}
