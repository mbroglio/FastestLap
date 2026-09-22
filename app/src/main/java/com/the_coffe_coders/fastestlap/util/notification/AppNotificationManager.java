package com.the_coffe_coders.fastestlap.util.notification;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.PowerManager;
import android.text.Html;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.messaging.FirebaseMessaging;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.util.Locale;
import java.util.Map;

/**
 * Centralized ManagerSingleton acting as the primary entry point for the notification system.
 * Configures FCM push notifications, notification channels, permissions, and token management,
 * while delegating specialized rendering to NewsNotificationManager and SessionNotificationManager.
 */
public class AppNotificationManager {

    private static final String TAG = "AppNotificationManager";

    public static final String CHANNEL_NEWS_ID = "fastestlap_news_v4";
    public static final String CHANNEL_SESSIONS_ID = "fastestlap_sessions_v4";
    public static final String CHANNEL_GENERAL_ID = "fastestlap_general_v4";

    public static final String PREF_NAME = "fastestlap_fcm_pref";
    public static final String KEY_FCM_TOKEN = "key_fcm_token";

    private static final String PREF_NOTIFIED_NEWS = "fastestlap_notified_news_urls";
    private static final String KEY_RECENT_NEWS_PREFIX = "recent_news_";
    private static final long NEWS_DUPLICATE_WINDOW_MS = 2 * 60 * 60 * 1000L; // 2 hours

    private static AppNotificationManager instance;

    public AppNotificationManager() {
    }

    public static synchronized AppNotificationManager getInstance() {
        if (instance == null) {
            instance = new AppNotificationManager();
        }
        return instance;
    }

    /**
     * Checks if a news article was already notified recently (within 2 hours)
     * to prevent duplicate notifications.
     */
    public boolean isDuplicateNews(Context context, String newsUrl, String title) {
        if (context == null) return false;
        String key = getNewsDeduplicationKey(newsUrl, title);
        if (key == null || key.isEmpty()) return false;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NOTIFIED_NEWS, Context.MODE_PRIVATE);
        long lastTime = prefs.getLong(KEY_RECENT_NEWS_PREFIX + key, 0);
        long now = System.currentTimeMillis();
        return (now - lastTime) < NEWS_DUPLICATE_WINDOW_MS;
    }

    /**
     * Records that a notification for a news article has been posted.
     */
    public void markNewsAsNotified(Context context, String newsUrl, String title) {
        if (context == null) return;
        String key = getNewsDeduplicationKey(newsUrl, title);
        if (key == null || key.isEmpty()) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NOTIFIED_NEWS, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_RECENT_NEWS_PREFIX + key, System.currentTimeMillis()).apply();
    }

    /**
     * Generates a deterministic deduplication key based on normalized article URL or alphanumeric title.
     */
    public String getNewsDeduplicationKey(String newsUrl, String title) {
        if (newsUrl != null && !newsUrl.trim().isEmpty()) {
            return String.valueOf(Math.abs(newsUrl.trim().toLowerCase(Locale.ROOT).hashCode()));
        }
        if (title != null && !title.trim().isEmpty()) {
            String norm = title.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
            return String.valueOf(Math.abs(norm.hashCode()));
        }
        return null;
    }

    /**
     * Wakes up the phone screen for ~4 seconds upon receiving a notification,
     * illuminating the display even when the device is locked or in standby mode.
     */
    public void wakeUpScreen(Context context) {
        if (context == null) return;
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && !powerManager.isInteractive()) {
                @SuppressLint("InvalidWakeLockTag")
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "FastestLap:NotificationWakeLock"
                );
                wakeLock.acquire(4000);
                Log.i(TAG, "💡 [WAKELOCK ACQUIRED]: Screen turned ON for incoming notification.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to wake up screen: " + e.getMessage());
        }
    }

    /**
     * Initializes Firebase Cloud Messaging:
     * 1. Creates notification channels.
     * 2. Subscribes device to standard broadcast topics ("all", "sessions") and user-preferred news topic.
     * 3. Fetches, logs, and caches the FCM registration token for Firebase Console targeting.
     */
    public void initFCM(Context context) {
        createNotificationChannels(context);

        // 1. Subscribe to broadcast topics
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.FCM_TOPIC_ALL)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to FCM topic: " + Constants.FCM_TOPIC_ALL);
                    }
                });

        FirebaseMessaging.getInstance().subscribeToTopic(Constants.FCM_TOPIC_SESSIONS)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to FCM topic: " + Constants.FCM_TOPIC_SESSIONS);
                    }
                });

        // 2. Subscribe to user-selected news source topic
        String currentSourceId = getSavedNewsSourceId(context);
        updateNewsTopicSubscription(currentSourceId);

        // 3. Retrieve and log the FCM registration token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed: ", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    saveFcmToken(context, token);

                    Log.i(TAG, "==================================================");
                    Log.i(TAG, "🔥 [FCM REGISTRATION TOKEN]:");
                    Log.i(TAG, "   " + token);
                    Log.i(TAG, "==================================================");

                    syncFcmTokenToRemote(token);
                });
    }

    /**
     * Updates topic subscriptions so the device ONLY receives news pushes from their chosen source.
     * Unsubscribes from all other source topics and generic news topic.
     */
    public void updateNewsTopicSubscription(String sourceId) {
        if (sourceId == null || sourceId.isEmpty()) {
            sourceId = Constants.NEWS_SOURCE_ID_MOTORSPORT;
        }
        String targetTopic = Constants.FCM_TOPIC_NEWS_PREFIX + sourceId;
        Log.i(TAG, "Updating news topic subscriptions. Target topic: " + targetTopic);

        java.util.List<String> allNewsTopics = java.util.List.of(
                Constants.FCM_TOPIC_NEWS_MOTORSPORT,
                Constants.FCM_TOPIC_NEWS_AUTOSPORT,
                Constants.FCM_TOPIC_NEWS_CRASH
        );

        for (String topic : allNewsTopics) {
            if (topic.equalsIgnoreCase(targetTopic)) {
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.i(TAG, "Subscribed to FCM topic: " + topic);
                            } else {
                                Log.w(TAG, "Failed subscribing to topic: " + topic, task.getException());
                            }
                        });
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Unsubscribed from FCM topic: " + topic);
                            }
                        });
            }
        }
        // Unsubscribe from legacy generic "news" topic to prevent duplicates
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.FCM_TOPIC_NEWS);
    }

    /**
     * Persists the user's selected news source into local preferences,
     * updates FCM topic subscriptions, and synchronizes with Firebase Realtime Database.
     */
    public void saveNewsSourcePreference(Context context, boolean isEnglish, int sourceIndex, String sourceName) {
        if (context == null) return;
        String sourceId = getSourceId(isEnglish, sourceIndex);
        if (sourceName == null || sourceName.isEmpty()) {
            sourceName = getSourceName(sourceId);
        }

        Log.i(TAG, "Saving news source preference: " + sourceName + " (id=" + sourceId + ", isEnglish=" + isEnglish + ", index=" + sourceIndex + ")");

        // 1. Save in SharedPreferences (shared)
        SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        sharedPrefs.edit()
                .putString(Constants.SHARED_PREFERENCES_NEWS_SOURCE, sourceName)
                .putString(Constants.SHARED_PREFERENCES_NEWS_SOURCE_ID, sourceId)
                .putBoolean(Constants.SHARED_PREFERENCES_NEWS_LANGUAGE, isEnglish)
                .putInt(Constants.SHARED_PREFERENCES_NEWS_INDEX, sourceIndex)
                .apply();

        // 2. Save in SharedPreferences (notification)
        SharedPreferences notifPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        notifPrefs.edit()
                .putString(Constants.SHARED_PREFERENCES_NEWS_SOURCE_ID, sourceId)
                .apply();

        // 3. Update FCM topic subscription
        updateNewsTopicSubscription(sourceId);

        // 4. Remote Realtime Database sync (if user is authenticated)
        try {
            com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String uid = currentUser.getUid();
                com.google.firebase.database.DatabaseReference userRef = com.google.firebase.database.FirebaseDatabase.getInstance(Constants.FIREBASE_REALTIME_DATABASE)
                        .getReference(Constants.FIREBASE_USERS_COLLECTION)
                        .child(uid);
                userRef.child(Constants.SHARED_PREFERENCES_NEWS_SOURCE).setValue(sourceName);
                userRef.child(Constants.SHARED_PREFERENCES_NEWS_SOURCE_ID).setValue(sourceId);

                String token = getCachedFcmToken(context);
                if (token != null) {
                    userRef.child("fcm_token").setValue(token);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not sync news source preference to Firebase Realtime Database: " + e.getMessage());
        }
    }

    public static String getSourceId(boolean isEnglish, int index) {
        if (isEnglish) {
            return (index == 1) ? Constants.NEWS_SOURCE_ID_CRASH : Constants.NEWS_SOURCE_ID_AUTOSPORT;
        } else {
            return Constants.NEWS_SOURCE_ID_MOTORSPORT;
        }
    }

    public static String getSourceName(String sourceId) {
        if (Constants.NEWS_SOURCE_ID_CRASH.equalsIgnoreCase(sourceId)) {
            return "Crash.net";
        } else if (Constants.NEWS_SOURCE_ID_AUTOSPORT.equalsIgnoreCase(sourceId)) {
            return "Autosport";
        } else {
            return "Motorsport";
        }
    }

    public static boolean isSourceEnglish(String sourceId) {
        return Constants.NEWS_SOURCE_ID_AUTOSPORT.equalsIgnoreCase(sourceId)
                || Constants.NEWS_SOURCE_ID_CRASH.equalsIgnoreCase(sourceId);
    }

    public static int getSourceIndex(String sourceId) {
        if (Constants.NEWS_SOURCE_ID_CRASH.equalsIgnoreCase(sourceId)) {
            return 1;
        }
        return 0;
    }

    public boolean hasSavedNewsSourcePreference(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        return prefs.contains(Constants.SHARED_PREFERENCES_NEWS_SOURCE_ID);
    }

    public String getSavedNewsSourceId(Context context) {
        if (context == null) return Constants.NEWS_SOURCE_ID_MOTORSPORT;
        SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        String sourceId = prefs.getString(Constants.SHARED_PREFERENCES_NEWS_SOURCE_ID, null);
        if (sourceId != null && !sourceId.isEmpty()) {
            return sourceId;
        }
        SharedPreferences notifPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sourceId = notifPrefs.getString(Constants.SHARED_PREFERENCES_NEWS_SOURCE_ID, null);
        if (sourceId != null && !sourceId.isEmpty()) {
            return sourceId;
        }
        boolean isEnglish = isDeviceLanguageEnglish(context);
        return isEnglish ? Constants.NEWS_SOURCE_ID_AUTOSPORT : Constants.NEWS_SOURCE_ID_MOTORSPORT;
    }

    public boolean isSavedNewsLanguageEnglish(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        if (prefs.contains(Constants.SHARED_PREFERENCES_NEWS_LANGUAGE)) {
            return prefs.getBoolean(Constants.SHARED_PREFERENCES_NEWS_LANGUAGE, false);
        }
        String sourceId = getSavedNewsSourceId(context);
        return isSourceEnglish(sourceId);
    }

    public int getSavedNewsSourceIndex(Context context) {
        if (context == null) return 0;
        SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        if (prefs.contains(Constants.SHARED_PREFERENCES_NEWS_INDEX)) {
            return prefs.getInt(Constants.SHARED_PREFERENCES_NEWS_INDEX, 0);
        }
        String sourceId = getSavedNewsSourceId(context);
        return getSourceIndex(sourceId);
    }

    public boolean isDeviceLanguageEnglish(Context context) {
        try {
            androidx.core.os.LocaleListCompat appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales();
            String langTag = appLocales.toLanguageTags();
            if (langTag != null && !langTag.isEmpty()) {
                return langTag.toLowerCase(Locale.ROOT).startsWith("en");
            }
            if (context != null) {
                String systemLang = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
                return systemLang != null && systemLang.toLowerCase(Locale.ROOT).startsWith("en");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking device language: " + e.getMessage());
        }
        return false;
    }

    public void syncFcmTokenToRemote(String token) {
        if (token == null || token.isEmpty()) return;
        try {
            com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                com.google.firebase.database.FirebaseDatabase.getInstance(Constants.FIREBASE_REALTIME_DATABASE)
                        .getReference(Constants.FIREBASE_USERS_COLLECTION)
                        .child(currentUser.getUid())
                        .child("fcm_token")
                        .setValue(token);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not sync token to remote database: " + e.getMessage());
        }
    }

    /**
     * Caches the FCM token in SharedPreferences for later use.
     */
    public void saveFcmToken(Context context, String token) {
        if (context == null || token == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
    }

    /**
     * Retrieves the cached FCM registration token.
     */
    public String getCachedFcmToken(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    /**
     * Returns the Uri for the custom notification sound (team_radio.mp3).
     * Uses stable name-based resource path to prevent stale ID issues across app rebuilds.
     */
    public static Uri getNotificationSoundUri(Context context) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/raw/team_radio");
    }

    /**
     * Returns the AudioAttributes configuration for notification playback.
     */
    public static AudioAttributes getNotificationAudioAttributes() {
        return new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
    }

    /**
     * Initializes notification channels for Android 8.0+ (API 26+).
     */
    public void createNotificationChannels(Context context) {
        if (context == null) return;
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) return;

        // Clean up legacy channels so Android resets settings to custom team_radio sound
        try {
            notificationManager.deleteNotificationChannel("fastestlap_news_channel");
            notificationManager.deleteNotificationChannel("fastestlap_session_channel");
            notificationManager.deleteNotificationChannel("fastestlap_news_v2");
            notificationManager.deleteNotificationChannel("fastestlap_sessions_v2");
            notificationManager.deleteNotificationChannel("fastestlap_general_v2");
            notificationManager.deleteNotificationChannel("fastestlap_news_v3");
            notificationManager.deleteNotificationChannel("fastestlap_sessions_v3");
            notificationManager.deleteNotificationChannel("fastestlap_general_v3");
        } catch (Exception ignored) {}

        Uri soundUri = getNotificationSoundUri(context);
        AudioAttributes audioAttributes = getNotificationAudioAttributes();
        long[] vibrationPattern = new long[]{0, 300, 200, 300};

        // 1. Channel for F1 News
        NotificationChannel newsChannel = new NotificationChannel(
                CHANNEL_NEWS_ID,
                context.getString(R.string.news_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        newsChannel.setDescription(context.getString(R.string.news_channel_description));
        newsChannel.enableVibration(true);
        newsChannel.setVibrationPattern(vibrationPattern);
        newsChannel.setShowBadge(true);
        newsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        newsChannel.setSound(soundUri, audioAttributes);

        // 2. Channel for Session Reminders (Race, Qualifying, Practice)
        NotificationChannel sessionChannel = new NotificationChannel(
                CHANNEL_SESSIONS_ID,
                context.getString(R.string.session_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        sessionChannel.setDescription(context.getString(R.string.session_channel_description));
        sessionChannel.enableVibration(true);
        sessionChannel.setVibrationPattern(vibrationPattern);
        sessionChannel.setShowBadge(true);
        sessionChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        sessionChannel.setSound(soundUri, audioAttributes);

        // 3. Channel for General Announcements from Firebase Console
        NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_GENERAL_ID,
                context.getString(R.string.general_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        generalChannel.setDescription(context.getString(R.string.general_channel_description));
        generalChannel.enableVibration(true);
        generalChannel.setVibrationPattern(vibrationPattern);
        generalChannel.setShowBadge(true);
        generalChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        generalChannel.setSound(soundUri, audioAttributes);

        notificationManager.createNotificationChannel(newsChannel);
        notificationManager.createNotificationChannel(sessionChannel);
        notificationManager.createNotificationChannel(generalChannel);

        Log.i(TAG, "Notification channels created successfully with team_radio custom sound (v4).");
    }

    /**
     * Checks if notification permission is granted (respects system settings and Android 13+ runtime permission).
     */
    public boolean hasNotificationPermission(Context context) {
        if (context == null) return false;
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Requests POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+).
     */
    public void requestNotificationPermission(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        200
                );
            }
        }
    }

    /**
     * Sanitizes raw RSS/Push HTML/XML content to clean plain text.
     */
    public String cleanHtmlDescription(String rawHtml) {
        if (rawHtml == null || rawHtml.trim().isEmpty()) {
            return "";
        }
        String text = Html.fromHtml(rawHtml, Html.FROM_HTML_MODE_LEGACY).toString();
        text = text.replaceAll("<[^>]*>", "");
        text = text.replace('\u00A0', ' ')
                   .replace("&nbsp;", " ")
                   .replaceAll("\\s+", " ")
                   .trim();

        if (text.length() > 250) {
            text = text.substring(0, 247) + "...";
        }
        return text;
    }

    /**
     * Displays a notification for a new F1 article.
     */
    public void showNewsNotification(Context context, String title, String summary, String newsUrl) {
        showNewsNotification(context, title, summary, newsUrl, null);
    }

    /**
     * Displays a notification for a new F1 article with an optional image banner.
     */
    @SuppressLint("MissingPermission")
    public void showNewsNotification(Context context, String title, String summary, String newsUrl, String imageUrl) {
        if (context == null) return;
        createNotificationChannels(context);

        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot show news notification — POST_NOTIFICATIONS permission not granted.");
            return;
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(() -> showNewsNotificationInternal(context, title, summary, newsUrl, imageUrl)).start();
        } else {
            showNewsNotificationInternal(context, title, summary, newsUrl, imageUrl);
        }
    }

    @SuppressLint("MissingPermission")
    private void showNewsNotificationInternal(Context context, String title, String summary, String newsUrl, String imageUrl) {
        String cleanTitle = cleanHtmlDescription(title != null ? title : context.getString(R.string.news_channel_name));
        String cleanSummary = cleanHtmlDescription(summary != null ? summary : "");

        // 1. Deduplication check: drop duplicate news notifications received within 2 hours
        if (isDuplicateNews(context, newsUrl, cleanTitle)) {
            Log.i(TAG, "Duplicate news notification suppressed for: " + cleanTitle);
            return;
        }
        markNewsAsNotified(context, newsUrl, cleanTitle);

        // 2. Wake up device screen from standby / lock screen
        wakeUpScreen(context);

        Intent intent = new Intent(context, HomePageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("EXTRA_TARGET_TAB", "news");
        if (newsUrl != null && !newsUrl.trim().isEmpty()) {
            intent.putExtra("EXTRA_NEWS_URL", newsUrl);
        }

        // Generate deterministic notification ID using normalized URL or title
        int notificationId;
        if (newsUrl != null && !newsUrl.trim().isEmpty()) {
            notificationId = Math.abs(newsUrl.trim().toLowerCase(Locale.ROOT).hashCode());
        } else {
            String norm = cleanTitle.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
            notificationId = norm.isEmpty() ? Math.abs(cleanTitle.hashCode()) : Math.abs(norm.hashCode());
        }
        if (notificationId <= 0) {
            notificationId = 1001;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Bitmap bannerBitmap = null;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                bannerBitmap = Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get();
            } catch (Exception e) {
                Log.w(TAG, "Could not load notification image from URL: " + imageUrl + " -> " + e.getMessage());
            }
        }

        Uri soundUri = getNotificationSoundUri(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_NEWS_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(cleanTitle)
                .setContentText(cleanSummary)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 300, 200, 300})
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (bannerBitmap != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(bannerBitmap)
                    .bigLargeIcon((Bitmap) null)
                    .setSummaryText(cleanSummary));
            builder.setLargeIcon(bannerBitmap);
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(cleanSummary));
            builder.setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon));
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        Log.i(TAG, "News notification posted (ID: " + notificationId + "): " + cleanTitle);
    }

    /**
     * Displays a high-priority notification alerting the user that a session is starting soon.
     */
    @SuppressLint("MissingPermission")
    public void showSessionNotification(Context context, String raceName, String sessionName, String sessionTime) {
        if (context == null) return;
        createNotificationChannels(context);

        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot show session notification — POST_NOTIFICATIONS permission not granted.");
            return;
        }

        String validRaceName = (raceName != null && !raceName.trim().isEmpty()) ? raceName : "Formula 1 Grand Prix";
        String validSessionName = (sessionName != null && !sessionName.trim().isEmpty()) ? sessionName : "Session";
        String validSessionTime = (sessionTime != null) ? sessionTime.trim() : "";

        // Wake up screen for session reminder
        wakeUpScreen(context);

        String localizedSessionName = getLocalizedSessionName(context, validSessionName);
        String contentText;
        if (!validSessionTime.isEmpty()) {
            contentText = localizedSessionName + " " + context.getString(R.string.session_starting_at) + " " + validSessionTime;
        } else {
            contentText = localizedSessionName;
        }

        Intent intent = new Intent(context, HomePageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("EXTRA_TARGET_TAB", "sessions");
        intent.putExtra("EXTRA_RACE_NAME", validRaceName);
        intent.putExtra("EXTRA_SESSION_NAME", validSessionName);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (validRaceName + validSessionName).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri sessionSoundUri = getNotificationSoundUri(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SESSIONS_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon))
                .setContentTitle(validRaceName)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(sessionSoundUri)
                .setVibrate(new long[]{0, 300, 200, 300})
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        int notificationId = (validRaceName + validSessionName).hashCode();
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        Log.i(TAG, "Session notification posted for: " + validRaceName + " - " + localizedSessionName);
    }

    private String getLocalizedSessionName(Context context, String sessionId) {
        if (sessionId == null) return "Session";
        boolean isItalian = "it".equalsIgnoreCase(Locale.getDefault().getLanguage());
        if (isItalian && Constants.SESSION_NAMES_ITA.containsKey(sessionId)) {
            return Constants.SESSION_NAMES_ITA.get(sessionId);
        } else if (Constants.SESSION_NAMES_ENG.containsKey(sessionId)) {
            return Constants.SESSION_NAMES_ENG.get(sessionId);
        }
        if (sessionId.startsWith("Practice") && sessionId.length() > 8) {
            return "Practice " + sessionId.substring(8);
        }
        return sessionId;
    }

    /**
     * Displays a general push notification sent from the Firebase Console or backend.
     */
    @SuppressLint("MissingPermission")
    public void showGeneralNotification(Context context, String title, String body, String imageUrl, Map<String, String> extraData) {
        if (context == null) return;
        createNotificationChannels(context);

        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot show general notification — POST_NOTIFICATIONS permission not granted.");
            return;
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(() -> showGeneralNotificationInternal(context, title, body, imageUrl, extraData)).start();
        } else {
            showGeneralNotificationInternal(context, title, body, imageUrl, extraData);
        }
    }

    @SuppressLint("MissingPermission")
    private void showGeneralNotificationInternal(Context context, String title, String body, String imageUrl, Map<String, String> extraData) {
        String cleanTitle = cleanHtmlDescription(title != null ? title : context.getString(R.string.app_name));
        String cleanBody = cleanHtmlDescription(body != null ? body : "");

        // Wake up screen for incoming general notification
        wakeUpScreen(context);

        Intent intent = new Intent(context, HomePageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (extraData != null) {
            for (Map.Entry<String, String> entry : extraData.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                cleanTitle.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Bitmap bannerBitmap = null;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                bannerBitmap = Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get();
            } catch (Exception e) {
                Log.w(TAG, "Could not load image for general notification: " + e.getMessage());
            }
        }

        Uri soundUri = getNotificationSoundUri(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(cleanTitle)
                .setContentText(cleanBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 300, 200, 300})
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (bannerBitmap != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(bannerBitmap)
                    .bigLargeIcon((Bitmap) null)
                    .setSummaryText(cleanBody));
            builder.setLargeIcon(bannerBitmap);
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(cleanBody));
            builder.setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon));
        }

        int notificationId = cleanTitle.hashCode() != 0 ? cleanTitle.hashCode() : (int) System.currentTimeMillis();
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        Log.i(TAG, "General FCM push notification posted: " + cleanTitle);
    }

    /**
     * Sends local test notifications (both news and session reminder) immediately,
     * allowing the user to verify permissions, channels, banners, sound, and vibration.
     */
    public void sendTestNotification(Context context) {
        if (context == null) return;
        createNotificationChannels(context);

        if (!hasNotificationPermission(context)) {
            android.widget.Toast.makeText(context, "⚠️ Permessi notifiche non concessi. Abilitali nelle impostazioni.", android.widget.Toast.LENGTH_LONG).show();
            openNotificationSettings(context);
            return;
        }

        // Clear test article from deduplication cache so multiple test button clicks always display
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NOTIFIED_NEWS, Context.MODE_PRIVATE);
            String testKey = getNewsDeduplicationKey("https://www.formula1.com", "🏎️ FastestLap - Notifica News Test");
            if (testKey != null) {
                prefs.edit().remove(KEY_RECENT_NEWS_PREFIX + testKey).apply();
            }
        } catch (Exception ignored) {}

        // 1. Trigger test news notification
        showNewsNotification(
                context,
                "🏎️ FastestLap - Notifica News Test",
                "Questa è una notifica di prova locale per verificare canali, suoni e banner delle notizie F1.",
                "https://www.formula1.com"
        );

        // 2. Trigger test session reminder
        showSessionNotification(
                context,
                "Gran Premio d'Italia - Monza",
                "Race",
                "15:00"
        );

        android.widget.Toast.makeText(context, "✅ Notifiche di test inviate!", android.widget.Toast.LENGTH_SHORT).show();
    }

    /**
     * Copies the current FCM Registration Token to the system clipboard and shows a confirmation Toast.
     */
    public void copyFcmTokenToClipboard(Context context) {
        if (context == null) return;
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String token = null;
            if (task.isSuccessful() && task.getResult() != null) {
                token = task.getResult();
                saveFcmToken(context, token);
            } else {
                token = getCachedFcmToken(context);
            }

            if (token != null && !token.trim().isEmpty()) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("FastestLap FCM Token", token);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
                android.widget.Toast.makeText(context, "📋 Token FCM copiato negli appunti!", android.widget.Toast.LENGTH_LONG).show();
                Log.i(TAG, "FCM Token copied: " + token);
            } else {
                android.widget.Toast.makeText(context, "Impossibile recuperare il token FCM al momento. Verifica la connessione.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Opens the system notification settings for this application.
     */
    public void openNotificationSettings(Context context) {
        if (context == null) return;
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Cannot open notification settings: " + e.getMessage());
        }
    }
}
