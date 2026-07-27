package com.the_coffe_coders.fastestlap;

import android.app.Application;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

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
    }
}
