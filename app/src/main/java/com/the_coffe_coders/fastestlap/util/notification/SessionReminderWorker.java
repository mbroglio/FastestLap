package com.the_coffe_coders.fastestlap.util.notification;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Background worker executed by WorkManager when an F1 session (FP1, Qualifying, Sprint, Race)
 * is starting or scheduled, triggering a high-priority session alert.
 */
public class SessionReminderWorker extends Worker {

    private static final String TAG = "SessionReminderWorker";

    public static final String KEY_RACE_NAME = "extra_race_name";
    public static final String KEY_SESSION_NAME = "extra_session_name";
    public static final String KEY_SESSION_TIME = "extra_session_time";

    public SessionReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "SessionReminderWorker executing.");
        try {
            String raceName = getInputData().getString(KEY_RACE_NAME);
            String sessionName = getInputData().getString(KEY_SESSION_NAME);
            String sessionTime = getInputData().getString(KEY_SESSION_TIME);

            if (raceName == null) raceName = "Formula 1 Grand Prix";
            if (sessionName == null) sessionName = "Session";
            if (sessionTime == null) sessionTime = "NOW";

            Log.i(TAG, "==================================================");
            Log.i(TAG, "🔔 [NOTIFICA SESSIONE IN CONSEGNA IN BACKGROUND]");
            Log.i(TAG, "   📍 Gara / Evento  : " + raceName);
            Log.i(TAG, "   🏁 Sessione       : " + sessionName);
            Log.i(TAG, "   🕒 Inizio         : " + sessionTime);
            Log.i(TAG, "==================================================");

            // Trigger high priority session notification via central manager
            AppNotificationManager.getInstance().showSessionNotification(
                    getApplicationContext(),
                    raceName,
                    sessionName,
                    sessionTime
            );

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error in SessionReminderWorker: " + e.getMessage(), e);
            return Result.failure();
        }
    }
}
