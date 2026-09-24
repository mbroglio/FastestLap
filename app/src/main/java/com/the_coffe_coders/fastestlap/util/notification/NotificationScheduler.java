package com.the_coffe_coders.fastestlap.util.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRaceClassic;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRaceSprint;

import org.threeten.bp.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Production utility helper responsible for background scheduling:
 * - Periodic RSS F1 news sync via WorkManager (background fetch).
 * - Exact F1 session reminders (15 minutes before start) via AlarmManager with exact wakeup,
 *   ensuring notifications fire on time even in deep Doze mode when the app is closed.
 */
public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    public static final String PERIODIC_NEWS_WORK_NAME = "fastestlap_news_periodic_work";

    /**
     * Schedules periodic background sync for news (every 1 hour).
     * Uses ExistingPeriodicWorkPolicy.KEEP so that opening the app does not constantly
     * reset or re-trigger the periodic background cycle.
     */
    public static void scheduleNewsCheck(Context context) {
        // Cancel any legacy WorkManager session reminders so they don't fire on app launch
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag("SessionReminderWorker");
        } catch (Exception ignored) {}

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest newsWorkRequest = new PeriodicWorkRequest.Builder(
                NewsBackgroundWorker.class,
                1, TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NEWS_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                newsWorkRequest
        );

        Log.i(TAG, "==================================================");
        Log.i(TAG, "📰 [WORKER NOTIZIE PERIODICO REGISTRATO]");
        Log.i(TAG, "   🔄 Frequenza    : Ogni 1 Ora");
        Log.i(TAG, "   🌐 Canale       : fastestlap_news_v4");
        Log.i(TAG, "==================================================");
    }

    /**
     * Cancels any active periodic background news sync on WorkManager,
     * as news notifications are now pushed centrally and in real-time by Firebase Cloud Functions.
     */
    public static void cancelNewsCheck(Context context) {
        if (context == null) return;
        try {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NEWS_WORK_NAME);
            Log.i(TAG, "Local periodic NewsBackgroundWorker cancelled in favor of FCM Cloud Function.");
        } catch (Exception e) {
            Log.w(TAG, "Failed to cancel periodic news check: " + e.getMessage());
        }
    }

    /**
    /**
     * Deprecated: Session reminders are now handled centrally and reliably via Firebase Cloud Functions
     * and Firebase Cloud Messaging (FCM) topics at -30 minutes and -5 minutes before session start.
     * Local AlarmManager reminders are disabled to prevent duplicate alerts and avoid Android Doze delays.
     */
    public static void scheduleSessionReminder(Context context, String raceName, String sessionName, String sessionTime, long sessionStartTimeMillis) {
        // No-op: Centralized FCM notifications handle session reminders directly from cloud.
        Log.d(TAG, "Local session scheduling skipped (handled centrally via FCM): " + raceName + " - " + sessionName);
    }

    /**
     * Deprecated: FCM notifications are pushed centrally from Firebase Cloud Functions;
     * no local alarms need to be restored on reboot.
     */
    public static void rescheduleAllUpcomingSessions(Context context) {
        // No-op: FCM push alerts are managed centrally on Firebase backend.
    }
}
