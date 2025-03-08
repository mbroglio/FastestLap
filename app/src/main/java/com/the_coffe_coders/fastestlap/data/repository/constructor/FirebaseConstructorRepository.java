package com.the_coffe_coders.fastestlap.data.repository.constructor;

import static com.the_coffe_coders.fastestlap.core.util.Constants.FIREBASE_REALTIME_DATABASE;
import static com.the_coffe_coders.fastestlap.core.util.Constants.FIREBASE_TEAMS_COLLECTION;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;


public class FirebaseConstructorRepository {
    private final FirebaseDatabase database;

    private final String TAG = "FirebaseConstructorRepository";

    public FirebaseConstructorRepository() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public void getConstructorData(String constructorId, ConstructorCallback callback) {
        DatabaseReference databaseReference = database.getReference(FIREBASE_TEAMS_COLLECTION).child(constructorId);
        Log.i(TAG, "FIREBASE => " + constructorId + "REFERENCE DB: " + databaseReference);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Constructor constructor = snapshot.getValue(Constructor.class);
                    if (constructor != null) {
                        callback.onSuccess(constructor);
                    } else {
                        callback.onFailure(new NullPointerException("Constructor data is null"));
                    }
                } else {
                    callback.onFailure(new NullPointerException("No constructor found"));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onFailure(error.toException());
            }
        });
    }

    public interface ConstructorCallback {
        void onSuccess(Constructor constructor);

        void onFailure(Exception exception);
    }
}
