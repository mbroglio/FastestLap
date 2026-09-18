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
        Log.i(TAG, "   🌐 Canale       : fastestlap_news_v3");
        Log.i(TAG, "==================================================");
    }

    /**
     * Schedules an exact reminder notification for an upcoming F1 session (15 minutes before start)
     * using AlarmManager setExactAndAllowWhileIdle to guarantee on-time delivery even when the app
     * is terminated and the device is in deep Doze mode.
     */
    public static void scheduleSessionReminder(Context context, String raceName, String sessionName, String sessionTime, long sessionStartTimeMillis) {
        long currentTimeMillis = System.currentTimeMillis();
        // Trigger 15 minutes before session start
        long notifyTimeMillis = sessionStartTimeMillis - TimeUnit.MINUTES.toMillis(15);
        long delayMillis = notifyTimeMillis - currentTimeMillis;

        if (delayMillis <= 0) {
            Log.w(TAG, "Session notification time is already in the past, skipping schedule for " + raceName + " - " + sessionName);
            return;
        }

        // Cancel any legacy WorkManager work for this session to purge old queued tasks
        String legacyWorkName = "session_reminder_" + (raceName + sessionName).hashCode();
        try {
            WorkManager.getInstance(context).cancelUniqueWork(legacyWorkName);
        } catch (Exception ignored) {}

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available, cannot schedule session reminder.");
            return;
        }

        Intent intent = new Intent(context, SessionAlarmReceiver.class);
        intent.putExtra(SessionAlarmReceiver.EXTRA_RACE_NAME, raceName);
        intent.putExtra(SessionAlarmReceiver.EXTRA_SESSION_NAME, sessionName);
        intent.putExtra(SessionAlarmReceiver.EXTRA_SESSION_TIME, sessionTime);
        intent.putExtra(SessionAlarmReceiver.EXTRA_START_TIME_MILLIS, sessionStartTimeMillis);

        int requestCode = (raceName + "_" + sessionName).hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule exact alarm waking up device from Doze mode (RTC_WAKEUP)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyTimeMillis, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyTimeMillis, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyTimeMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to setAndAllowWhileIdle: " + e.getMessage());
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyTimeMillis, pendingIntent);
        }

        long delaySeconds = delayMillis / 1000;
        long delayMinutes = delaySeconds / 60;
        long delayHours = delayMinutes / 60;
        long delayDays = delayHours / 24;

        String formattedDelay = delayDays > 0
                ? String.format(java.util.Locale.getDefault(), "%dd %dh %dm", delayDays, delayHours % 24, delayMinutes % 60)
                : String.format(java.util.Locale.getDefault(), "%dh %dm %ds", delayHours, delayMinutes % 60, delaySeconds % 60);

        Log.i(TAG, "==================================================");
        Log.i(TAG, "⏰ [PROMEMORIA SESSIONE REGISTRATO CON ALARM_MANAGER]");
        Log.i(TAG, "   📍 Gara / Evento  : " + raceName);
        Log.i(TAG, "   🏁 Sessione       : " + sessionName);
        Log.i(TAG, "   🕒 Ora Inizio     : " + sessionTime);
        Log.i(TAG, "   🔔 Sveglia Tra    : " + formattedDelay + " (15 min prima)");
        Log.i(TAG, "==================================================");
    }

    /**
     * Reschedules all upcoming session alarms from the local Room database.
     * Invoked on device reboot (ACTION_BOOT_COMPLETED) to restore all lost alarms.
     */
    public static void rescheduleAllUpcomingSessions(Context context) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppRoomDatabase db = AppRoomDatabase.getDatabase(context);
                List<WeeklyRaceClassic> classicRaces = db.weeklyRaceClassicDAO().getAllRaces();
                List<WeeklyRaceSprint> sprintRaces = db.weeklyRaceSprintDAO().getAllRaces();

                List<WeeklyRace> allRaces = new ArrayList<>();
                if (classicRaces != null) allRaces.addAll(classicRaces);
                if (sprintRaces != null) allRaces.addAll(sprintRaces);

                long now = System.currentTimeMillis();

                for (WeeklyRace race : allRaces) {
                    List<Session> sessions = race.getSessions();
                    if (sessions == null) continue;

                    for (Session s : sessions) {
                        if (s.getStartDateTime() != null) {
                            long sessionStartTimeMillis = s.getStartDateTime()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli();

                            long notifyTimeMillis = sessionStartTimeMillis - TimeUnit.MINUTES.toMillis(15);
                            if (notifyTimeMillis > now) {
                                String sessionId = s.getClass().getSimpleName();
                                if (s instanceof Practice) {
                                    sessionId = ((Practice) s).getPractice();
                                }

                                String raceName = race.getRaceName() != null ? race.getRaceName() : "Formula 1 Grand Prix";
                                scheduleSessionReminder(context, raceName, sessionId, s.getStartingTime(), sessionStartTimeMillis);
                            }
                        }
                    }
                }
                Log.i(TAG, "All upcoming session alarms successfully restored after reboot.");
            } catch (Exception e) {
                Log.e(TAG, "Error restoring session alarms after reboot: " + e.getMessage(), e);
            }
        });
    }
}
