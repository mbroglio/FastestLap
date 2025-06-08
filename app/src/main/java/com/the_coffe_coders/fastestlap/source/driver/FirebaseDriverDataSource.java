package com.the_coffe_coders.fastestlap.source.driver;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.repository.driver.DriverCallback;

public class FirebaseDriverDataSource implements DriverDataSource {
    private static final String TAG = "FirebaseDriverDataSource";
    private static FirebaseDriverDataSource instance;
    private final FirebaseDatabase database;

    public FirebaseDriverDataSource() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public static synchronized FirebaseDriverDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseDriverDataSource();
        }
        return instance;
    }

    @Override
    public void getDriver(String driverId, DriverCallback callback) {
        Log.i(TAG, "Fetching driver from Firebase with ID: " + driverId);
        DatabaseReference databaseReference = database.getReference("drivers").child(driverId);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Driver driver = snapshot.getValue(Driver.class);
                    if (driver != null) {
                        Log.i(TAG, "Successfully retrieved driver from Firebase: " + driver);
                        callback.onDriverLoaded(driver);
                    } else {
                        Log.e(TAG, "Driver data is null for ID: " + driverId);
                        callback.onError(new Exception("Driver data is null"));
                    }
                } else {
                    Log.e(TAG, "No driver found for ID: " + driverId);
                    callback.onError(new Exception("No driver found"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase request cancelled: " + error.getMessage());
                callback.onError(new Exception("Firebase error: " + error.getMessage()));
            }
        });
    }

}
