package com.the_coffe_coders.fastestlap.util.notification;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumptech.glide.Glide;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;

/**
 * Specialized notification manager for F1 News alerts.
 * Handles image downloading via Glide, BigPictureStyle rendering, and intent payload routing.
 */
public class NewsNotificationManager {

    private static final String TAG = "NewsNotificationManager";
    private static NewsNotificationManager instance;

    private NewsNotificationManager() {
    }

    public static synchronized NewsNotificationManager getInstance() {
        if (instance == null) {
            instance = new NewsNotificationManager();
        }
        return instance;
    }

    public void showNewsNotification(Context context, String title, String summary, String newsUrl) {
        showNewsNotification(context, title, summary, newsUrl, null);
    }

    @SuppressLint("MissingPermission")
    public void showNewsNotification(Context context, String title, String summary, String newsUrl, String imageUrl) {
        AppNotificationManager mainManager = AppNotificationManager.getInstance();
        mainManager.createNotificationChannels(context);

        if (!mainManager.hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot show news notification — POST_NOTIFICATIONS permission not granted.");
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Abilita i permessi di notifica dalle impostazioni del telefono!", Toast.LENGTH_LONG).show());
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
        AppNotificationManager mainManager = AppNotificationManager.getInstance();
        String cleanTitle = mainManager.cleanHtmlDescription(title);
        String cleanSummary = mainManager.cleanHtmlDescription(summary);

        Intent intent = new Intent(context, HomePageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("EXTRA_NEWS_URL", newsUrl);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                cleanTitle != null ? cleanTitle.hashCode() : 0,
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AppNotificationManager.CHANNEL_NEWS_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(cleanTitle)
                .setContentText(cleanSummary)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
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
        }

        int notificationId = cleanTitle != null ? cleanTitle.hashCode() : (int) System.currentTimeMillis();
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        Log.i(TAG, "News notification posted: " + cleanTitle);
    }
}
