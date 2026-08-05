package com.the_coffe_coders.fastestlap.util.notification;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Production utility helper responsible for scheduling WorkManager background tasks:
 * periodic RSS F1 news sync and delayed F1 session reminders.
 */
public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    public static final String PERIODIC_NEWS_WORK_NAME = "fastestlap_news_periodic_work";

    /**
     * Schedules periodic background sync for news (every 1 hour).
     */
    public static void scheduleNewsCheck(Context context) {
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
        Log.i(TAG, "   🌐 Canale       : fastestlap_news_v2");
        Log.i(TAG, "==================================================");
    }

    /**
     * Schedules a one-time reminder notification for an upcoming F1 session (15 minutes before start).
     */
    public static void scheduleSessionReminder(Context context, String raceName, String sessionName, String sessionTime, long sessionStartTimeMillis) {
        long currentTimeMillis = System.currentTimeMillis();
        // Trigger 15 minutes before session start
        long notifyTimeMillis = sessionStartTimeMillis - TimeUnit.MINUTES.toMillis(15);
        long delayMillis = notifyTimeMillis - currentTimeMillis;

        if (delayMillis <= 0) {
            Log.w(TAG, "Session time is already in the past, skipping schedule for " + sessionName);
            return;
        }

        Data inputData = new Data.Builder()
                .putString(SessionReminderWorker.KEY_RACE_NAME, raceName)
                .putString(SessionReminderWorker.KEY_SESSION_NAME, sessionName)
                .putString(SessionReminderWorker.KEY_SESSION_TIME, sessionTime)
                .build();

        OneTimeWorkRequest reminderRequest = new OneTimeWorkRequest.Builder(SessionReminderWorker.class)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        String uniqueWorkName = "session_reminder_" + (raceName + sessionName).hashCode();
        WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                reminderRequest
        );

        long delaySeconds = delayMillis / 1000;
        long delayMinutes = delaySeconds / 60;
        long delayHours = delayMinutes / 60;
        long delayDays = delayHours / 24;

        String formattedDelay = delayDays > 0
                ? String.format(java.util.Locale.getDefault(), "%dd %dh %dm", delayDays, delayHours % 24, delayMinutes % 60)
                : String.format(java.util.Locale.getDefault(), "%dh %dm %ds", delayHours, delayMinutes % 60, delaySeconds % 60);

        Log.i(TAG, "==================================================");
        Log.i(TAG, "⏰ [PROMEMORIA SESSIONE REGISTRATO IN WORKMANAGER]");
        Log.i(TAG, "   📍 Gara / Evento  : " + raceName);
        Log.i(TAG, "   🏁 Sessione       : " + sessionName);
        Log.i(TAG, "   🕒 Ora Inizio     : " + sessionTime);
        Log.i(TAG, "   🔔 Notifica Tra   : " + formattedDelay + " (15 min prima)");
        Log.i(TAG, "==================================================");
    }
}
