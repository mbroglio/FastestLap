package com.the_coffe_coders.fastestlap.util.notification;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;

/**
 * Specialized notification manager for F1 Session Start Reminders (Race, Qualifying, Sprint, FP1/FP2/FP3).
 * Handles high-priority event category alerts with localized session start time strings.
 */
public class SessionNotificationManager {

    private static final String TAG = "SessionNotificationManager";
    private static SessionNotificationManager instance;

    private SessionNotificationManager() {
    }

    public static synchronized SessionNotificationManager getInstance() {
        if (instance == null) {
            instance = new SessionNotificationManager();
        }
        return instance;
    }

    /**
     * Displays a high-priority notification alerting the user that a session is starting soon.
     */
    @SuppressLint("MissingPermission")
    public void showSessionNotification(Context context, String raceName, String sessionName, String sessionTime) {
        AppNotificationManager mainManager = AppNotificationManager.getInstance();
        mainManager.createNotificationChannels(context);

        if (mainManager.hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot show session notification — POST_NOTIFICATIONS permission not granted.");
            return;
        }

        String contentText = sessionName + " " + context.getString(R.string.session_starting_at) + " " + sessionTime;

        Intent intent = new Intent(context, HomePageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (raceName + sessionName).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AppNotificationManager.CHANNEL_SESSIONS_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(raceName)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        int notificationId = (raceName + sessionName).hashCode();
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        Log.i(TAG, "Session notification posted for: " + raceName + " - " + sessionName);
    }
}
