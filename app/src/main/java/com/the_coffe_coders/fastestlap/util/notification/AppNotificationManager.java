package com.the_coffe_coders.fastestlap.util.notification;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.Html;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.the_coffe_coders.fastestlap.R;

/**
 * Centralized ManagerSingleton acting as the primary entry point for the notification system.
 * Configures channels and permissions, and delegates specialized notification rendering
 * to NewsNotificationManager and SessionNotificationManager.
 */
public class AppNotificationManager {

    private static final String TAG = "AppNotificationManager";

    public static final String CHANNEL_NEWS_ID = "fastestlap_news_v2";
    public static final String CHANNEL_SESSIONS_ID = "fastestlap_sessions_v2";

    private static AppNotificationManager instance;

    private final NewsNotificationManager newsManager;
    private final SessionNotificationManager sessionManager;

    private AppNotificationManager() {
        this.newsManager = NewsNotificationManager.getInstance();
        this.sessionManager = SessionNotificationManager.getInstance();
    }

    public static synchronized AppNotificationManager getInstance() {
        if (instance == null) {
            instance = new AppNotificationManager();
        }
        return instance;
    }

    /**
     * Initializes notification channels for Android 8.0+ (API 26+).
     */
    public void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager == null) return;

            // 1. Channel for F1 News
            NotificationChannel newsChannel = new NotificationChannel(
                    CHANNEL_NEWS_ID,
                    context.getString(R.string.news_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            newsChannel.setDescription(context.getString(R.string.news_channel_description));
            newsChannel.enableVibration(true);
            newsChannel.setShowBadge(true);
            newsChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            // 2. Channel for Session Reminders (Race, Qualifying, Practice)
            NotificationChannel sessionChannel = new NotificationChannel(
                    CHANNEL_SESSIONS_ID,
                    context.getString(R.string.session_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            sessionChannel.setDescription(context.getString(R.string.session_channel_description));
            sessionChannel.enableVibration(true);
            sessionChannel.setShowBadge(true);
            sessionChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            notificationManager.createNotificationChannel(newsChannel);
            notificationManager.createNotificationChannel(sessionChannel);

            Log.i(TAG, "Notification channels created successfully.");
        }
    }

    /**
     * Checks if POST_NOTIFICATIONS permission is granted (Android 13+ / API 33+).
     */
    public boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Requests POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+).
     */
    public void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }

    /**
     * Sanitizes raw RSS HTML/XML content to clean plain text.
     */
    public String cleanHtmlDescription(String rawHtml) {
        if (rawHtml == null || rawHtml.trim().isEmpty()) {
            return "";
        }
        String text;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            text = Html.fromHtml(rawHtml, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            text = Html.fromHtml(rawHtml).toString();
        }

        // Remove residual HTML/XML tags
        text = text.replaceAll("<[^>]*>", "");

        // Clean non-breaking spaces and normalize whitespace
        text = text.replace('\u00A0', ' ')
                   .replace("&nbsp;", " ")
                   .replaceAll("\\s+", " ")
                   .trim();

        if (text.length() > 200) {
            text = text.substring(0, 197) + "...";
        }
        return text;
    }

    /**
     * Displays a notification for a new F1 article.
     */
    public void showNewsNotification(Context context, String title, String summary, String newsUrl) {
        newsManager.showNewsNotification(context, title, summary, newsUrl);
    }

    /**
     * Displays a notification for a new F1 article with an optional image banner.
     */
    public void showNewsNotification(Context context, String title, String summary, String newsUrl, String imageUrl) {
        newsManager.showNewsNotification(context, title, summary, newsUrl, imageUrl);
    }

    /**
     * Displays a high-priority notification alerting the user that a session is starting soon.
     */
    public void showSessionNotification(Context context, String raceName, String sessionName, String sessionTime) {
        sessionManager.showSessionNotification(context, raceName, sessionName, sessionTime);
    }
}
