package com.the_coffe_coders.fastestlap;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.FirebaseDatabase;
import com.the_coffe_coders.fastestlap.util.notification.AppNotificationManager;
import com.the_coffe_coders.fastestlap.util.notification.NotificationScheduler;

public class FastestLapApplication extends Application {
    private static final String TAG = "FastestLapApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Enable Firebase Realtime Database offline persistence.
            // This ensures all database reads (drivers, constructors, nations, tracks)
            // are cached locally on disk by the Firebase SDK and returned instantly
            // without requiring a network round-trip.
            FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE).setPersistenceEnabled(true);
            Log.i(TAG, "Firebase Realtime Database offline persistence enabled.");
        } catch (Exception e) {
            Log.w(TAG, "Failed to enable Firebase persistence: " + e.getMessage());
        }

        // Initialize centralized notification channels & FCM push notifications
        try {
            AppNotificationManager.getInstance().initFCM(this);
            NotificationScheduler.cancelNewsCheck(this);
            Log.i(TAG, "Notification system & FCM push notification apparatus initialized.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize notification system: " + e.getMessage());
        }

        // Automatically clear all active notifications from the notification shade and
        // dismiss the launcher badge whenever the user enters / accesses the app.
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                AppNotificationManager.getInstance().clearAllNotifications(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                AppNotificationManager.getInstance().clearAllNotifications(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
}
