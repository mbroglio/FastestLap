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
import android.os.Build;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

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
     * ----------------------------------------------------------------------------------------------
     * IMAGE LOADING AND COLORS
     * ----------------------------------------------------------------------------------------------
     */

    public static void loadImageWithGlide(Context context, String url, ImageView imageView, Runnable onSuccess) {
        loadImage(context, url, imageView, onSuccess, 0);
    }

    public static void loadSequenceOfImagesWithGlide(Context context, String[] urls, ImageView[] imageViews, Runnable onSuccess) {
        if (urls.length != imageViews.length) {
            throw new IllegalArgumentException("The length of urls and imageViews must be the same");
        }

        for (int i = 0; i < urls.length; i++) {
            int nextIndex = i + 1;
            if (i == urls.length - 1) {
                loadImage(context, urls[i], imageViews[i], onSuccess, 0);
            } else {
                loadImage(context, urls[i], imageViews[i], () ->
                        loadImage(context, urls[nextIndex], imageViews[nextIndex], onSuccess, 0), 0);
            }
        }
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
            if (onSuccess != null) onSuccess.run();
            return;
        }

        // Track how many images have loaded
        final int[] loadedCount = {0};
        final int totalImages = urls.length;

        Runnable checkComplete = () -> {
            synchronized (loadedCount) {
                loadedCount[0]++;
                if (loadedCount[0] == totalImages && onSuccess != null) {
                    onSuccess.run();
                }
            }
        };

        // Load all images in parallel
        for (int i = 0; i < urls.length; i++) {
            loadImage(context, urls[i], imageViews[i], checkComplete, 0);
        }
    }

    private static void loadImage(Context context, String url, ImageView imageView, Runnable onSuccess, int retryCount) {
        Log.i("Glide", "Loading image: " + url);

        NetworkUtils networkLiveData = new NetworkUtils(context);

        if (url != null && !url.isEmpty()) {
            Glide.with(context)
                    .load(url)
                    .thumbnail(0.25f)  // Load 25% quality version first for instant display
                    .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache both original and resized
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("Glide", "Image loading failed: " + url);

                            // We handled the error
                            if (networkLiveData.isConnected()) {
                                // We handled the error
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
                            return true; // Return true to prevent Glide from handling the error (since we retry)
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.i("Glide", "Image loaded successfully: ");
                            if (onSuccess != null) {
                                onSuccess.run();
                            }
                            return false; // Return false to allow Glide to handle setting the drawable on the target
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
                            if (onSuccess != null) {
                                onSuccess.run();
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
        String normalized = s.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{Alnum}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
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
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equalsIgnoreCase("en-GB")) {
            UIUtils.singleSetTextViewText(Constants.SESSION_NAMES_ENG.getOrDefault(sessionId, context.getString(R.string.unknown)), sessionTypeTextView);

        } else if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equalsIgnoreCase("it-IT")) {
            UIUtils.singleSetTextViewText(Constants.SESSION_NAMES_ITA.getOrDefault(sessionId, context.getString(R.string.unknown)), sessionTypeTextView);
        }
    }

    public static void translateSessionDay(Context context, TextView sessionDayTextView, String sessionId) {
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equalsIgnoreCase("en-GB")) {
            UIUtils.singleSetTextViewText(Constants.SESSION_DAY_ENG.getOrDefault(sessionId, context.getString(R.string.unknown)), sessionDayTextView);

        } else if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equalsIgnoreCase("it-IT")) {
            UIUtils.singleSetTextViewText(Constants.SESSION_DAY_ITA.getOrDefault(sessionId, context.getString(R.string.unknown)), sessionDayTextView);
        }
    }

    public static void translateEventDateInterval(String eventDate, TextView eventDateTextView) {
        String newEventDate = eventDate.split(" ")[0] + " " +
                eventDate.split(" ")[1] + " " +
                eventDate.split(" ")[2] + " ";

        if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equalsIgnoreCase("en-GB")) {
            newEventDate += eventDate.split(" ")[3].toUpperCase();
        } else if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equalsIgnoreCase("it-IT")) {
            newEventDate += Objects.requireNonNull(Constants.MONTH_ENG_TO_ITA.get(eventDate.split(" ")[3].toLowerCase())).toUpperCase();
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
        if (AppCompatDelegate.getApplicationLocales().get(0) == null) {
            LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(Constants.DEFAULT_LANGUAGE);
            AppCompatDelegate.setApplicationLocales(appLocale);
        }

        AppCompatDelegate.setApplicationLocales(AppCompatDelegate.getApplicationLocales());
    }


    /*
     * ----------------------------------------------------------------------------------------------
     * TIME HELPERS
     * ----------------------------------------------------------------------------------------------
     */

    @RequiresApi(api = Build.VERSION_CODES.S)
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

            long seconds = duration.toSeconds();

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
            e.printStackTrace();
            return "Invalid date format";
        }
    }

    // SYSTEM_UI_FLAG_FULLSCREEN: Hide the status bar
    // SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN: Let the layout expand into status bar
    // SYSTEM_UI_FLAG_LAYOUT_STABLE: avoid abrupt layout changes during toggling of status and navigation bars
    // SYSTEM_UI_FLAG_HIDE_NAVIGATION: Hide the navigation bar
    // SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION: Let the layout expand into navigation bar
}
