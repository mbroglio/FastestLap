package com.the_coffe_coders.fastestlap.util.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver responsible for handling exact session reminder alarms triggered by AlarmManager,
 * ensuring notifications fire on time even when the app is completely closed and the device is in Doze mode.
 * Also handles device reboots (ACTION_BOOT_COMPLETED) to reschedule upcoming session reminders.
 */
public class SessionAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "SessionAlarmReceiver";

    public static final String EXTRA_RACE_NAME = "extra_race_name";
    public static final String EXTRA_SESSION_NAME = "extra_session_name";
    public static final String EXTRA_SESSION_TIME = "extra_session_time";
    public static final String EXTRA_START_TIME_MILLIS = "extra_start_time_millis";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();

        // 1. Device reboot: restore all upcoming session alarms from local Room database
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.i(TAG, "Device rebooted (BOOT_COMPLETED). Rescheduling upcoming session reminders...");
            NotificationScheduler.rescheduleAllUpcomingSessions(context.getApplicationContext());
            return;
        }

        // 2. Exact session alarm triggered
        String raceName = intent.getStringExtra(EXTRA_RACE_NAME);
        String sessionName = intent.getStringExtra(EXTRA_SESSION_NAME);
        String sessionTime = intent.getStringExtra(EXTRA_SESSION_TIME);
        long startTimeMillis = intent.getLongExtra(EXTRA_START_TIME_MILLIS, 0);

        long now = System.currentTimeMillis();

        // Safety check: if the session has already started or finished (e.g. phone was powered off during the session),
        // discard the alarm to avoid flooding the user with stale notifications.
        if (startTimeMillis > 0 && now >= startTimeMillis) {
            Log.w(TAG, "Session has already started or passed (" + raceName + " - " + sessionName + "). Skipping stale alarm.");
            return;
        }

        if (raceName == null) raceName = "Formula 1 Grand Prix";
        if (sessionName == null) sessionName = "Session";
        if (sessionTime == null) sessionTime = "NOW";

        Log.i(TAG, "==================================================");
        Log.i(TAG, "⏰ [EXACT ALARM TRIGGERED VIA ALARM_MANAGER]");
        Log.i(TAG, "   📍 Gara / Evento  : " + raceName);
        Log.i(TAG, "   🏁 Sessione       : " + sessionName);
        Log.i(TAG, "   🕒 Inizio         : " + sessionTime);
        Log.i(TAG, "==================================================");

        AppNotificationManager.getInstance().wakeUpScreen(context);

        AppNotificationManager.getInstance().showSessionNotification(
                context.getApplicationContext(),
                raceName,
                sessionName,
                sessionTime
        );
    }
}
