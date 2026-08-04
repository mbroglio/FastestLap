package com.the_coffe_coders.fastestlap;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.app.Application;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;

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

        // Initialize centralized notification channels & periodic background sync for production
        try {
            com.the_coffe_coders.fastestlap.util.notification.AppNotificationManager.getInstance().createNotificationChannels(this);
            com.the_coffe_coders.fastestlap.util.notification.NotificationScheduler.scheduleNewsCheck(this);
            Log.i(TAG, "Notification system & background news scheduler initialized.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize notification system: " + e.getMessage());
        }
    }
}




