package com.the_coffe_coders.fastestlap.data.repository.driver;

import static com.the_coffe_coders.fastestlap.core.util.Constants.FIREBASE_DRIVERS_COLLECTION;
import static com.the_coffe_coders.fastestlap.core.util.Constants.FIREBASE_REALTIME_DATABASE;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

public class FirebaseDriverRepository {
    private final FirebaseDatabase database;

    public FirebaseDriverRepository() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public void getDriver(String driverId, DriverCallback callback) {
        DatabaseReference databaseReference = database.getReference(FIREBASE_DRIVERS_COLLECTION).child(driverId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Driver driver = snapshot.getValue(Driver.class);
                    if (driver != null) {
                        callback.onSuccess(driver);
                    } else {
                        callback.onFailure(new NullPointerException("Driver data is null"));
                    }
                } else {
                    callback.onFailure(new NullPointerException("No driver found"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.toException());
            }
        });
    }

    public interface DriverCallback {
        void onSuccess(Driver driver);

        void onFailure(Exception exception);
    }
}