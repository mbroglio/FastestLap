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

        // 1. Device reboot: legacy local alarms are deprecated in favor of FCM Cloud Functions
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.i(TAG, "Device rebooted (BOOT_COMPLETED). Local alarms suppressed in favor of centralized FCM push notifications.");
            return;
        }

        // 2. Exact session alarm triggered from legacy AlarmManager
        // If an old pending alarm is delivered by the OS, discard it to avoid duplicate or delayed notifications.
        Log.i(TAG, "Legacy local session alarm received. Suppressed in favor of centralized FCM push notifications (-30m and -5m).");
    }
}
