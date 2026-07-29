package com.the_coffe_coders.fastestlap.util.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class UIUtils {


    /*
     * ----------------------------------------------------------------------------------------------
     * WINDOW MANAGEMENT
     * ----------------------------------------------------------------------------------------------
     */

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

    /*
     * -----------------------------------------------------------------------------------------------
     * NAVIGATION
     * -----------------------------------------------------------------------------------------------
     */

    public static void manualToolbarTitleUpdateWithNavigation(NavController navController, AppCompatActivity activity) {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (activity.getSupportActionBar() != null) {
                CharSequence title = destination.getLabel();
                activity.getSupportActionBar().setTitle(title);
                // Always show the navigation icon (back button)
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        });

    }


    /*
     * ----------------------------------------------------------------------------------------------
     * IMAGE LOADING AND COLORS
     * ----------------------------------------------------------------------------------------------
     */

    public static void loadImageWithGlide(Context context, String url, ImageView imageView, Runnable onSuccess) {
        loadImage(context, url, imageView, onSuccess, 0);
    }

    /**
     * Starts downloading the image at {@code url} into Glide's disk cache without
     * displaying it anywhere. Call this as early as possible (e.g. when you first
     * receive the URL) so the cache is warm by the time the real load starts.
     */
    public static void preloadImage(Context context, String url) {
        if (url == null || url.isEmpty()) return;
        // Guard against destroyed activities
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) return;
        }
        // Use applicationContext so the decoded bitmap is pinned in the app-scoped
        // Glide RequestManager and never evicted when a fragment/activity is destroyed.
        // This makes every return visit to the home fragment an instant memory-cache hit
        // instead of a fresh Firebase Storage fetch.
        Glide.with(context.getApplicationContext())
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload();
    }

    public static void preloadImage(Context context, String url, Runnable onComplete) {
        if (url == null || url.isEmpty()) {
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
            return;
        }

        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                if (onComplete != null) {
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
                return;
            }
        }

        // Use applicationContext so the decoded bitmap survives fragment/activity lifecycle
        // changes and is available as a memory-cache hit on the next home fragment visit.
        Glide.with(context.getApplicationContext())
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
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

        // Start the chain with the first image
        loadImageSequentially(context, urls, imageViews, 0, onSuccess);
    }

    private static void loadImageSequentially(Context context, String[] urls, ImageView[] imageViews, int index, Runnable onSuccess) {
        if (index >= urls.length) {
            // All images loaded, call final callback
            if (onSuccess != null) {
                new Handler(Looper.getMainLooper()).post(onSuccess);
            }
            return;
        }

        // Load current image with callback to load next image
        loadImage(context, urls[index], imageViews[index], () -> {
            // Load next image in sequence
            loadImageSequentially(context, urls, imageViews, index + 1, onSuccess);
        }, 0);
    }

    /**
     * Load multiple images in parallel (faster than sequence)
     * All images load simultaneously, onSuccess called when all complete
     */
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

        // Track how many images have fully loaded (full-quality, not thumbnail)
        final int[] loadedCount = {0};
        final int totalImages = urls.length;

        Runnable checkComplete = () -> {
            synchronized (loadedCount) {
                loadedCount[0]++;
                if (loadedCount[0] == totalImages && onSuccess != null) {
                    // Post to Handler to escape the callback context
                    // This prevents IllegalStateException if onSuccess triggers another Glide load
                    new Handler(Looper.getMainLooper()).post(onSuccess);
                }
            }
        };

        // Load all images in parallel
        for (int i = 0; i < urls.length; i++) {
            loadImage(context, urls[i], imageViews[i], checkComplete, 0);
        }
    }

    /**
     * Loads an image into an ImageView asynchronously without any completion callback.
     * Use this when you want to display data immediately and let images fill in on their own.
     * Unlike loadImagesInParallel(), this does NOT block any completion signal.
     */
    public static void loadImageAsync(Context context, String url, ImageView imageView) {
        if (imageView == null) return;
        if (url == null || url.isEmpty()) return;
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) return;
        }
        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }


    private static void loadImage(Context context, String url, ImageView imageView, Runnable onSuccess, int retryCount) {
        Log.i("Glide", "Loading image: " + url);

        // Guard: if the context is a destroyed Activity, skip the load to avoid the
        // "You cannot start a load for a destroyed activity" crash that occurs when an
        // async callback (posted via Handler) fires after the user has navigated away.
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                Log.w("Glide", "Skipping image load — activity is destroyed: " + url);
                if (onSuccess != null) {
                    new Handler(Looper.getMainLooper()).post(onSuccess);
                }
                return;
            }
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = cm != null ? cm.getActiveNetwork() : null;
        NetworkCapabilities nc = (activeNetwork != null) ? cm.getNetworkCapabilities(activeNetwork) : null;
        final boolean isConnected = nc != null
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);


        if (url != null && !url.isEmpty()) {
            // Ensure the onSuccess callback fires exactly once per loadImage() call.
            // .thumbnail(0.25f) causes Glide to invoke onResourceReady twice:
            //   1st call: low-res thumbnail (isFirstResource = true)
            //   2nd call: full-quality image  (isFirstResource = false)
            // Without this guard, loadImagesInParallel's counter would be incremented
            // twice per image, firing the completion callback before all images are ready.
            final boolean[] callbackFired = {false};

            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache both original and resized
                    .listener(new RequestListener<>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("Glide", "Image loading failed: " + url);

                            synchronized (callbackFired) {
                                if (callbackFired[0]) return true;
                                callbackFired[0] = true;
                            }

                            if (isConnected) {
                                if (retryCount <= Constants.MAX_RETRY_COUNT) {
                                    Log.i("Glide", "Retrying image load: " + url + " - retry count: " + retryCount);
                                    new Handler(Looper.getMainLooper()).post(() -> loadImage(context, url, imageView, onSuccess, retryCount + 1));
                                } else {
                                    Log.e("Glide", "Max retry count reached for image: " + url);
                                    manageContentLoadError(imageView, null, context, onSuccess, 0);
                                }
                            } else {
                                manageContentLoadError(imageView, null, context, onSuccess, 0);
                            }
                            return true; // We handled the error
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.i("Glide", "Full image loaded (firing callback): " + url);

                            synchronized (callbackFired) {
                                if (callbackFired[0])
                                    return false; // callback already fired
                                callbackFired[0] = true;
                            }

                            if (onSuccess != null) {
                                // Post to Handler to escape the callback context.
                                // This prevents IllegalStateException if onSuccess triggers another Glide load.
                                new Handler(Looper.getMainLooper()).post(onSuccess);
                            }
                            return false; // Let Glide display the resource
                        }
                    })
                    .into(imageView);
        } else {
            Log.e("Glide", "URL is null");
            manageContentLoadError(imageView, null, context, onSuccess, 0);
        }
    }

    public static void loadImageInEventCardWithAlpha(Context context, String url, LinearLayout card, Runnable onSuccess, int alpha) {
        loadImageAlpha(context, url, card, onSuccess, alpha, 0);
    }

    private static void loadImageAlpha(Context context, String url, LinearLayout card, Runnable onSuccess, int alpha, int retryCount) {
        // Guard: skip load if the associated Activity is already destroyed.
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                Log.w("Glide", "Skipping alpha image load — activity is destroyed: " + url);
                if (onSuccess != null) {
                    new Handler(Looper.getMainLooper()).post(onSuccess);
                }
                return;
            }
        }

        if (url != null && !url.isEmpty()) {
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
                            if (onSuccess != null) {
                                // Post to Handler to escape the callback context
                                // This prevents IllegalStateException if onSuccess triggers another Glide load
                                new Handler(Looper.getMainLooper()).post(onSuccess);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Use default image if loading fails
                            Drawable defaultImage = ContextCompat.getDrawable(context, R.drawable.content_not_found_icon);
                            if (defaultImage != null) {
                                defaultImage.setAlpha(76);
                            }
                            card.setBackground(defaultImage);
                            if (onSuccess != null) {
                                // Post to Handler to escape the callback context
                                // This prevents IllegalStateException if onSuccess triggers another Glide load
                                new Handler(Looper.getMainLooper()).post(onSuccess);
                            }
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            Log.w("Glide", "Image loading failed: " + url);
                            if (retryCount <= Constants.MAX_RETRY_COUNT) {
                                Log.i("Glide", "Retrying image load: " + url + " - retry count: " + retryCount);
                                new Handler(Looper.getMainLooper()).post(() -> loadImageAlpha(context, url, card, onSuccess, alpha, retryCount + 1));
                            } else {
                                Log.e("Glide", "Max retry count reached for image: " + url);
                                manageContentLoadError(null, card, context, onSuccess, 1);
                            }
                        }
                    });
        } else {
            Log.e("Glide", "URL is null");
            manageContentLoadError(null, card, context, onSuccess, 1);
        }
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

    private static void manageContentLoadError(ImageView imageView, LinearLayout layout, Context context, Runnable onSuccess, int contentType) {
        switch (contentType) {
            case 0: // Image
                Log.e("Glide", "Image loading failed, setting backup image");
                Drawable errorImage = AppCompatResources.getDrawable(context, R.drawable.content_not_found_icon);
                imageView.setImageDrawable(errorImage);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
            case 1: // Layout
                Log.e("UIUtils", "Layout loading failed, setting backup background");
                layout.setBackgroundColor(context.getColor(R.color.timer_gray));
                break;
            default:
                Log.e("UIUtils", "Unknown content type for error handling");
        }

        if (onSuccess != null) {
            onSuccess.run();
        }
    }


    /*
     * ----------------------------------------------------------------------------------------------
     * TEXTS AND STRINGS FORMATTING
     * ----------------------------------------------------------------------------------------------
     */

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
        if (texts.length != textViews.length) {
            throw new IllegalArgumentException("The length of texts and textViews must be the same");
        }

        for (int i = 0; i < texts.length; i++) {
            setTextViewText(texts[i], textViews[i]);
        }
    }

    private static void setTextViewText(String text, TextView textView) {
        if (text != null) {
            textView.setText(text);
        }
    }

    public static String formatXmlText(String xmlRawString) {
        String formattedString;
        if (xmlRawString != null) {
            // convert <br>, <br/> and <br /> (case-insensitive) to newlines
            formattedString = xmlRawString.replaceAll("(?i)<br\\s*/?>", "\n");

            // lowercased version for case-insensitive searches
            String lower = formattedString.toLowerCase(Locale.ENGLISH);

            int aIndex = lower.indexOf("<a");
            int readAlsoIndex = lower.indexOf("read also");

            int cutIndex = -1;
            if (aIndex != -1 && readAlsoIndex != -1) {
                cutIndex = Math.min(aIndex, readAlsoIndex);
            } else if (aIndex != -1) {
                cutIndex = aIndex;
            } else if (readAlsoIndex != -1) {
                cutIndex = readAlsoIndex;
            }

            if (cutIndex != -1) {
                formattedString = formattedString.substring(0, cutIndex).trim();
            } else {
                formattedString = formattedString.trim();
            }
        } else {
            formattedString = "";
        }
        return formattedString;
    }

    // check if a string (inputString) contains a substring (idString)
    public static boolean containsIdString(String inputString, String idString) {
        if (inputString == null || idString == null) return false;
        String normalizedInput = normalizeForMatch(inputString);
        String normalizedId = normalizeForMatch(idString);
        if (normalizedId.isEmpty()) return false;
        return normalizedInput.contains(normalizedId);
    }

    // check if a string (inputString) contains a substring (idString) and return the idString if it does
    public static String getContainedIdString(String inputString, String idString) {
        if (containsIdString(inputString, idString)) {
            return idString;
        }
        return null;
    }

    // normalize a string for matching
    private static String normalizeForMatch(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{Alnum}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // find the key in a map that matches the input string
    public static String findMatchingKeyInMap(String inputString, Map<String, ?> map) {
        if (inputString == null || map == null || map.isEmpty()) return null;
        String bestMatch = null;
        String normalizedInput = normalizeForMatch(inputString);

        for (String key : map.keySet()) {
            if (key == null) continue;
            String normalizedKey = normalizeForMatch(key);
            if (normalizedKey.isEmpty()) continue;
            if (normalizedInput.contains(normalizedKey)) {
                if (bestMatch == null || normalizedKey.length() > normalizeForMatch(bestMatch).length()) {
                    bestMatch = key;
                }
            }
        }
        return bestMatch;
    }

    public static Object getFromMap(String key, Map<String, ?> map) {
        if (!map.containsKey(key)) return "-";
        return map.get(key);
    }


    /*
     * ----------------------------------------------------------------------------------------------
     * TRANSLATIONS
     * ----------------------------------------------------------------------------------------------
     */

    public static void translateSchedule(Context context, TextView sessionTypeTextView, TextView sessionDayTextView, String sessionId) {
        translateSessionType(context, sessionTypeTextView, sessionId);
        translateSessionDay(context, sessionDayTextView, sessionId);
    }

    public static void translateSessionType(Context context, TextView sessionTypeTextView, String sessionId) {
        String langTags = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (langTags != null && langTags.toLowerCase(Locale.ROOT).startsWith("it")) {
            UIUtils.singleSetTextViewText(Constants.SESSION_NAMES_ITA.getOrDefault(sessionId, context.getString(R.string.unknown)), sessionTypeTextView);
        } else {
            UIUtils.singleSetTextViewText(Constants.SESSION_NAMES_ENG.getOrDefault(sessionId, context.getString(R.string.unknown)), sessionTypeTextView);
        }
    }

    public static void translateSessionDay(Context context, TextView sessionDayTextView, String sessionId) {
        String langTags = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (langTags != null && langTags.toLowerCase(Locale.ROOT).startsWith("it")) {
            UIUtils.singleSetTextViewText(Constants.SESSION_DAY_ITA.getOrDefault(sessionId, context.getString(R.string.unknown)), sessionDayTextView);
        } else {
            UIUtils.singleSetTextViewText(Constants.SESSION_DAY_ENG.getOrDefault(sessionId, context.getString(R.string.unknown)), sessionDayTextView);
        }
    }

    public static void translateEventDateInterval(String eventDate, TextView eventDateTextView) {
        String newEventDate = eventDate.split(" ")[0] + " " +
                eventDate.split(" ")[1] + " " +
                eventDate.split(" ")[2] + " ";

        String langTags = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (langTags != null && langTags.toLowerCase(Locale.ROOT).startsWith("it")) {
            newEventDate += Objects.requireNonNull(Constants.MONTH_ENG_TO_ITA.get(eventDate.split(" ")[3].toLowerCase(Locale.ROOT))).toUpperCase(Locale.ROOT);
        } else {
            newEventDate += eventDate.split(" ")[3].toUpperCase(Locale.ROOT);
        }

        UIUtils.singleSetTextViewText(newEventDate, eventDateTextView);
    }

    public static void translateMonth(String abbr, TextView textView, boolean abbreviation) {
        if (abbreviation) {
            setTextViewTextWithCondition(AppCompatDelegate.getApplicationLocales().toLanguageTags().equalsIgnoreCase("it-IT"),
                    Constants.MONTH_ABBR_ENG_TO_ITA.get(abbr),
                    abbr,
                    textView);
        } else {
            setTextViewTextWithCondition(AppCompatDelegate.getApplicationLocales().toLanguageTags().equalsIgnoreCase("it-IT"),
                    Constants.MONTH_ENG_TO_ITA.get(abbr),
                    abbr,
                    textView);
        }
    }


    /*
     * ----------------------------------------------------------------------------------------------
     * LANGUAGE MANAGEMENT
     * ----------------------------------------------------------------------------------------------
     */

    public static void setAppLocale() {
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        if (currentLocales.isEmpty() || currentLocales.get(0) == null) {
            java.util.Locale defaultLocale = java.util.Locale.forLanguageTag(Constants.DEFAULT_LANGUAGE);
            java.util.Locale systemLocale = java.util.Locale.getDefault();
            if (!systemLocale.getLanguage().equalsIgnoreCase(defaultLocale.getLanguage())) {
                LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(Constants.DEFAULT_LANGUAGE);
                AppCompatDelegate.setApplicationLocales(appLocale);
            }
        }
    }


    /*
     * ----------------------------------------------------------------------------------------------
     * TIME HELPERS
     * ----------------------------------------------------------------------------------------------
     */

    public static String getTimeAgo(String dateString, Context context) {
        long SECONDS_PER_MINUTE = 60;
        long SECONDS_PER_HOUR = 3600;
        long SECONDS_PER_DAY = 86400;
        long SECONDS_PER_MONTH = 2592000;
        long SECONDS_PER_YEAR = 31536000;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

        try {
            ZonedDateTime pastTime = ZonedDateTime.parse(dateString, formatter);
            ZonedDateTime now = ZonedDateTime.now(pastTime.getZone());
            Duration duration = Duration.between(pastTime, now);

            long seconds = duration.getSeconds();

            if (seconds < 60) {
                return context.getString(R.string.just_now);
            }

            if (seconds < SECONDS_PER_MINUTE * 60) {
                long minutes = seconds / SECONDS_PER_MINUTE;
                return context.getResources().getQuantityString(R.plurals.minutes_ago, (int) minutes, minutes);
            }
            if (seconds < SECONDS_PER_DAY) {
                long hours = seconds / SECONDS_PER_HOUR;
                return context.getResources().getQuantityString(R.plurals.hours_ago, (int) hours, hours);
            }
            if (seconds < SECONDS_PER_MONTH) {
                long days = seconds / SECONDS_PER_DAY;
                return context.getResources().getQuantityString(R.plurals.days_ago, (int) days, days);
            }
            if (seconds < SECONDS_PER_YEAR) {
                long months = seconds / SECONDS_PER_MONTH;
                return context.getResources().getQuantityString(R.plurals.months_ago, (int) months, months);
            }

            long years = seconds / SECONDS_PER_YEAR;
            return context.getResources().getQuantityString(R.plurals.years_ago, (int) years, years);

        } catch (DateTimeParseException e) {
            // Handle invalid date string
            Log.e("UIUtils", "Error parsing date string: " + e.getMessage());
            return "Invalid date format";
        }
    }



    /*
     * ----------------------------------------------------------------------------------------------
     * TACHOMETER UTILITIES
     * ----------------------------------------------------------------------------------------------
     */

    /**
     * Updates tachometer views with driver statistics
     *
     * @param context          The context (usually Activity)
     * @param driver           The driver object containing statistics
     * @param winTachometer    The tachometer view for win percentage
     * @param podiumTachometer The tachometer view for podium percentage
     */
    public static void updateTachometers(Context context, Driver driver,
                                         TachometerView winTachometer, TachometerView podiumTachometer) {
        String TAG = "UIUtils.updateTachometers";

        // Set tachometer colors based on team
        int teamColor = R.color.app_primary_red; // Default color
        if (driver.getTeam_id() != null) {
            try {
                Integer color = Constants.TEAM_COLOR.get(driver.getTeam_id());
                if (color != null) {
                    teamColor = color;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting team color: " + e.getMessage());
            }
        }

        int finalColor = ContextCompat.getColor(context, teamColor);
        winTachometer.setColor(finalColor);
        podiumTachometer.setColor(finalColor);

        // Get total races from driver's gps_entered field
        int totalRaces = 0;
        try {
            String gpsEnteredStr = driver.getGps_entered();
            if (gpsEnteredStr != null && !gpsEnteredStr.equals("N/A") && !gpsEnteredStr.isEmpty()) {
                totalRaces = Integer.parseInt(gpsEnteredStr);
                Log.i(TAG, "Total GPs entered from driver data: " + totalRaces);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing gps_entered: " + e.getMessage());
        }

        // If gps_entered is not available, fall back to estimation
        if (totalRaces == 0 && driver.getDriver_history() != null && !driver.getDriver_history().isEmpty()) {
            int seasonsCount = driver.getDriver_history().size();
            totalRaces = seasonsCount * 20; // Estimate: 20 races per season
            Log.i(TAG, "Estimated total races from history: " + totalRaces);
        }

        // If still no data, set to 0
        if (totalRaces == 0) {
            winTachometer.setPercentage(0f);
            winTachometer.setLabel(ContextCompat.getString(context, R.string.wins));
            podiumTachometer.setPercentage(0f);
            podiumTachometer.setLabel(ContextCompat.getString(context, R.string.podiums));
            Log.i(TAG, "No race data available");
            return;
        }

        // Get total wins from best_result field
        // Format: "3(x34)" means best result is 3rd, achieved 34 times
        // If first number is 1, it means wins
        int totalWins = 0;
        try {
            String bestResult = driver.getBest_result();
            if (bestResult != null && !bestResult.equals("N/A") && !bestResult.isEmpty()) {
                // Extract the position number (before the opening parenthesis)
                int openParenIndex = bestResult.indexOf('(');
                if (openParenIndex > 0) {
                    String positionStr = bestResult.substring(0, openParenIndex).trim();
                    int position = Integer.parseInt(positionStr);

                    // If position is 1, extract the win count
                    if (position == 1) {
                        // Extract count from "(x34)" format
                        int xIndex = bestResult.indexOf('x');
                        int closeParenIndex = bestResult.indexOf(')');
                        if (xIndex > 0 && closeParenIndex > xIndex) {
                            String countStr = bestResult.substring(xIndex + 1, closeParenIndex).trim();
                            totalWins = Integer.parseInt(countStr);
                            Log.i(TAG, "Total wins from best_result: " + totalWins);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing best_result: " + e.getMessage());
        }

        // Get total podiums from podiums field
        int totalPodiums = 0;
        try {
            String podiumsStr = driver.getPodiums();
            if (podiumsStr != null && !podiumsStr.equals("N/A") && !podiumsStr.isEmpty()) {
                totalPodiums = Integer.parseInt(podiumsStr);
                Log.i(TAG, "Total podiums from driver data: " + totalPodiums);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing podiums: " + e.getMessage());
        }

        // Calculate percentages
        float winPercentage = totalRaces > 0 ? (totalWins * 100f / totalRaces) : 0f;
        float podiumPercentage = totalRaces > 0 ? (totalPodiums * 100f / totalRaces) : 0f;

        // Update tachometers
        winTachometer.setPercentage(winPercentage);
        winTachometer.setLabel("Wins");

        podiumTachometer.setPercentage(podiumPercentage);
        podiumTachometer.setLabel("Podiums");

        Log.i(TAG, "Win %: " + winPercentage + ", Podium %: " + podiumPercentage +
                ", Total Races: " + totalRaces + ", Wins: " + totalWins + ", Podiums: " + totalPodiums);
    }

    /**
     * Updates tachometer views with team statistics
     *
     * @param context          The context (usually Activity)
     * @param constructor      The constructor object containing statistics
     * @param winTachometer    The tachometer view for win percentage
     * @param podiumTachometer The tachometer view for podium percentage
     */
    public static void updateTachometers(Context context, Constructor constructor,
                                         TachometerView winTachometer, TachometerView podiumTachometer) {
        String TAG = "UIUtils.updateTachometers";

        // Set tachometer colors based on team
        int teamColor = R.color.app_primary_red; // Default color

        try {
            Integer color = Constants.TEAM_COLOR.get(constructor.getConstructorId());
            if (color != null) {
                teamColor = color;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting team color: " + e.getMessage());
        }


        int finalColor = ContextCompat.getColor(context, teamColor);
        winTachometer.setColor(finalColor);
        podiumTachometer.setColor(finalColor);

        // Get total races from driver's gps_entered field
        int totalRaces = 0;
        try {
            String gpsEnteredStr = constructor.getGps_entered();
            if (gpsEnteredStr != null && !gpsEnteredStr.equals("N/A") && !gpsEnteredStr.isEmpty()) {
                totalRaces = Integer.parseInt(gpsEnteredStr) * 2;
                Log.i(TAG, "Total GPs entered from driver data: " + totalRaces);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing gps_entered: " + e.getMessage());
        }

        // If gps_entered is not available, fall back to estimation
        if (totalRaces == 0 && constructor.getTeam_history() != null && !constructor.getTeam_history().isEmpty()) {
            int seasonsCount = constructor.getTeam_history().size();
            totalRaces = seasonsCount * 20 * 2; // Estimate: 20 races per season
            Log.i(TAG, "Estimated total races from history: " + totalRaces);
        }

        // If still no data, set to 0
        if (totalRaces == 0) {
            winTachometer.setPercentage(0f);
            winTachometer.setLabel(ContextCompat.getString(context, R.string.wins));
            podiumTachometer.setPercentage(0f);
            podiumTachometer.setLabel(ContextCompat.getString(context, R.string.podiums));
            Log.i(TAG, "No race data available");
            return;
        }

        // Get total wins from best_result field
        // Format: "3(x34)" means best result is 3rd, achieved 34 times
        // If first number is 1, it means wins
        int totalWins = 0;
        try {
            String winsStr = constructor.getWins();
            if (winsStr != null && !winsStr.equals("N/A") && !winsStr.isEmpty()) {
                totalWins = Integer.parseInt(winsStr);
                Log.i(TAG, "Total podiums from driver data: " + totalWins);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing podiums: " + e.getMessage());
        }

        // Get total podiums from podiums field
        int totalPodiums = 0;
        try {
            String podiumsStr = constructor.getPodiums();
            if (podiumsStr != null && !podiumsStr.equals("N/A") && !podiumsStr.isEmpty()) {
                totalPodiums = Integer.parseInt(podiumsStr);
                Log.i(TAG, "Total podiums from driver data: " + totalPodiums);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing podiums: " + e.getMessage());
        }

        // Calculate percentages
        float winPercentage = totalRaces > 0 ? (totalWins * 100f / totalRaces) : 0f;
        float podiumPercentage = totalRaces > 0 ? (totalPodiums * 100f / totalRaces) : 0f;

        // Update tachometers
        winTachometer.setPercentage(winPercentage);
        winTachometer.setLabel("Wins");

        podiumTachometer.setPercentage(podiumPercentage);
        podiumTachometer.setLabel("Podiums");

        Log.i(TAG, "Win %: " + winPercentage + ", Podium %: " + podiumPercentage +
                ", Total Races: " + totalRaces + ", Wins: " + totalWins + ", Podiums: " + totalPodiums);
    }

    // SYSTEM_UI_FLAG_FULLSCREEN: Hide the status bar
    // SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN: Let the layout expand into status bar
    // SYSTEM_UI_FLAG_LAYOUT_STABLE: avoid abrupt layout changes during toggling of status and navigation bars
    // SYSTEM_UI_FLAG_HIDE_NAVIGATION: Hide the navigation bar
    // SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION: Let the layout expand into navigation bar
}
