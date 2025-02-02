package com.the_coffe_coders.fastestlap.repository.driver;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_DRIVERS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

public class FirebaseDriverRepository {
    private final FirebaseDatabase database;

    public interface DriverCallback {
        void onSuccess(Driver driver);
        void onFailure(Exception exception);
    }

    public FirebaseDriverRepository() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public void getDriverData(String driverId, DriverCallback callback) {
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
            public void onCancelled(DatabaseError error) {
                callback.onFailure(error.toException());
            }
        });
    }
}