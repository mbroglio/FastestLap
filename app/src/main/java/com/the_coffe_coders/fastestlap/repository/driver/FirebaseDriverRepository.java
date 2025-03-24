package com.the_coffe_coders.fastestlap.repository.driver;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_DRIVERS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.concurrent.CompletableFuture;

public class FirebaseDriverRepository {
    private static final String TAG = "FirebaseDriverRepository";
    private final FirebaseDatabase database;

    public FirebaseDriverRepository() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public CompletableFuture<Result> getDriver(String driverId) {
        CompletableFuture<Result> future = new CompletableFuture<>();

        Log.i(TAG, "Fetching driver from Firebase with ID: " + driverId);
        DatabaseReference databaseReference = database.getReference(FIREBASE_DRIVERS_COLLECTION).child(driverId);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Driver driver = snapshot.getValue(Driver.class);
                    if (driver != null) {
                        Log.i(TAG, "Successfully retrieved driver from Firebase: " + driver);
                        future.complete(new Result.DriverSuccess(driver));
                    } else {
                        Log.e(TAG, "Driver data is null for ID: " + driverId);
                        future.complete(new Result.Error("Driver data is null"));
                    }
                } else {
                    Log.e(TAG, "No driver found for ID: " + driverId);
                    future.complete(new Result.Error("No driver found"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase request cancelled: " + error.getMessage());
                future.complete(new Result.Error("Firebase error: " + error.getMessage()));
            }
        });

        // Set a timeout for the future
        setupFutureTimeout(future, 30000); // 30 seconds timeout

        return future;
    }

    /**
     * Sets a timeout for a CompletableFuture if it's not completed within the specified time
     */
    private void setupFutureTimeout(CompletableFuture<Result> future, long timeoutMs) {
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(timeoutMs);
                if (!future.isDone()) {
                    Log.w(TAG, "Firebase request timed out after " + timeoutMs + "ms");
                    future.complete(new Result.Error("Firebase request timed out"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }
}