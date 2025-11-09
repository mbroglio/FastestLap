package com.the_coffe_coders.fastestlap.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.ui.bio.ConstructorBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.TrackBioActivity;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.ui.event.PastEventsActivity;
import com.the_coffe_coders.fastestlap.ui.event.UpcomingEventsActivity;
import com.the_coffe_coders.fastestlap.ui.event.fragment.QualifyingResultsFragment;
import com.the_coffe_coders.fastestlap.ui.event.fragment.RaceAndSprintResultsFragment;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.home.fragment.NewsFragment;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.WelcomeActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.fragment.ForgotPasswordFragment;
import com.the_coffe_coders.fastestlap.ui.welcome.fragment.SignUpFragment;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;


public class UIUtils {
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
            int nextIndex = i + 1;
            if (i == urls.length - 1) {
                loadImage(context, urls[i], imageViews[i], onSuccess, 0);
            } else {
                loadImage(context, urls[i], imageViews[i], () ->
                        loadImage(context, urls[nextIndex], imageViews[nextIndex], onSuccess, 0), 0);
            }
        }
    }

    private static void loadImage(Context context, String url, ImageView imageView, Runnable onSuccess, int retryCount) {
        Log.i("Glide", "Loading image: " + url);
        if (url != null && !url.isEmpty()) {
            Glide.with(context)
                    .load(url)
                    .into(new CustomTarget<Drawable>() {

                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            Log.i("Glide", "Image loaded successfully: ");
                            imageView.setImageDrawable(resource);
                            if (onSuccess != null) {
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
                                new Handler(Looper.getMainLooper()).post(() -> loadImage(context, url, imageView, onSuccess, retryCount + 1));
                            } else {
                                Log.e("Glide", "Max retry count reached for image: " + url);
                                manageContentLoadError(imageView, null, context, onSuccess, 0);
                            }
                        }

                    });
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

    private static void manageContentLoadError(ImageView imageView, LinearLayout layout, Context context, Runnable onSuccess, int contentType) {
        switch (contentType) {
            case 0: // Image
                Log.e("Glide", "Image loading failed, setting backup image");
                Drawable errorImage = AppCompatResources.getDrawable(context, R.drawable.content_not_found_icon);
                imageView.setImageDrawable(errorImage);
                imageView.setScaleType(ImageView.ScaleType.CENTER);
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
        context.startActivity(intent);
    }

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

    public static String formatXmlText(String xmlRawString){
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

    public static void setAppLocale() {
        if (AppCompatDelegate.getApplicationLocales().get(0) == null) {
            LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(Constants.DEFAULT_LANGUAGE);
            AppCompatDelegate.setApplicationLocales(appLocale);
        }

        AppCompatDelegate.setApplicationLocales(AppCompatDelegate.getApplicationLocales());
    }

    public static void navigateToHomePage(Context context) {
        Intent intent = new Intent(context, HomePageActivity.class);
        context.startActivity(intent);
    }

    public static void navigateToBioPage(Context context, String id, int bioType) {
        Intent intent;

        switch (bioType) {
            case 0:
                intent = new Intent(context, ConstructorBioActivity.class);
                intent.putExtra("TEAM_ID", id);
                context.startActivity(intent);
                break;
            case 1:
                intent = new Intent(context, DriverBioActivity.class);
                intent.putExtra("DRIVER_ID", id);
                context.startActivity(intent);
                break;
            case 2:
                intent = new Intent(context, TrackBioActivity.class);
                String circuitId = id.split("&")[0];
                String grandPrixName = id.split("&")[1];

                intent.putExtra("CIRCUIT_ID", circuitId);
                intent.putExtra("GRAND_PRIX_NAME", grandPrixName);
                context.startActivity(intent);
                break;
        }
    }

    public static void navigateToStandingsPage(Context context, String id, int standingsType) {
        Intent intent;

        switch (standingsType) {
            case 0:
                intent = new Intent(context, ConstructorsStandingActivity.class);
                intent.putExtra("TEAM_ID", id);
                context.startActivity(intent);
                break;
            case 1:
                intent = new Intent(context, DriversStandingActivity.class);
                intent.putExtra("DRIVER_ID", id);
                context.startActivity(intent);
                break;
        }
    }

    public static void navigateToEventsListPage(Context context, int eventType) {
        Intent intent;

        switch (eventType) {
            case 0:
                intent = new Intent(context, UpcomingEventsActivity.class);
                context.startActivity(intent);
                break;
            case 1:
                intent = new Intent(context, PastEventsActivity.class);
                context.startActivity(intent);
                break;
        }
    }

    public static void navigateToEventPage(Context context, String circuitId) {
        Intent intent = new Intent(context, EventActivity.class);
        intent.putExtra("CIRCUIT_ID", circuitId);
        context.startActivity(intent);
    }

    public static void navigateToWelcomePage(Context context) {
        Intent intent = new Intent(context, WelcomeActivity.class);
        context.startActivity(intent);
    }

    public static void showRaceResultsDialog(FragmentManager fragmentManager, Race race, int sessionType) {
        switch (sessionType) {
            case 0:
                RaceAndSprintResultsFragment raceAndSprintResultsFragment = new RaceAndSprintResultsFragment();
                Bundle args = new Bundle();
                args.putParcelable("RACE", race);
                raceAndSprintResultsFragment.setArguments(args);
                raceAndSprintResultsFragment.show(fragmentManager, "RaceResultsFragment");
                break;
            case 1:
                QualifyingResultsFragment qualifyingResultsFragment = new QualifyingResultsFragment();
                Bundle qualifyingArgs = new Bundle();
                qualifyingArgs.putParcelable("RACE", race);
                qualifyingResultsFragment.setArguments(qualifyingArgs);
                qualifyingResultsFragment.show(fragmentManager, "QualifyingResultsFragment");
                break;
        }
    }

    public static void showNewsDialog(FragmentManager fragmentManager, String language) {
        NewsFragment newsFragment = new NewsFragment();
        newsFragment.show(fragmentManager, "NewsFragment");
        Bundle args = new Bundle();
        args.putString("currentLanguage", language);
        newsFragment.setArguments(args);
    }


    public static void showWelcomeDialogs(FragmentManager fragmentManager, int dialogType) {
        switch (dialogType) {
            case 0:
                SignUpFragment signUpFragment = new SignUpFragment();
                signUpFragment.show(fragmentManager, "SignUpFragment");
                break;
            case 1:
                ForgotPasswordFragment forgotPasswordFragment = new ForgotPasswordFragment();
                forgotPasswordFragment.show(fragmentManager, "ForgotPasswordFragment");
                break;
        }
    }

    public static String getTimeAgo(String dateString) {
        long SECONDS_PER_MINUTE = 60;
        long SECONDS_PER_HOUR = 3600;
        long SECONDS_PER_DAY = 86400;
        long SECONDS_PER_MONTH = 2592000; // Approx. 30 days
        long SECONDS_PER_YEAR = 31536000;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

        try {
            ZonedDateTime pastTime = ZonedDateTime.parse(dateString, formatter);
            ZonedDateTime now = ZonedDateTime.now(pastTime.getZone()); // Get "now" in the same zone
            Duration duration = Duration.between(pastTime, now);

            long seconds = duration.toSeconds();

            if (seconds < 60) {
                return "just now";
            }

            if (seconds < SECONDS_PER_MINUTE * 60) {
                long minutes = seconds / SECONDS_PER_MINUTE;
                return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
            }
            if (seconds < SECONDS_PER_DAY) {
                long hours = seconds / SECONDS_PER_HOUR;
                return hours == 1 ? "1 hour ago" : hours + " hours ago";
            }
            if (seconds < SECONDS_PER_MONTH) {
                long days = seconds / SECONDS_PER_DAY;
                return days == 1 ? "1 day ago" : days + " days ago";
            }
            if (seconds < SECONDS_PER_YEAR) {
                long months = seconds / SECONDS_PER_MONTH;
                return months == 1 ? "1 month ago" : months + " months ago";
            }

            long years = seconds / SECONDS_PER_YEAR;
            return years == 1 ? "1 year ago" : years + " years ago";

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