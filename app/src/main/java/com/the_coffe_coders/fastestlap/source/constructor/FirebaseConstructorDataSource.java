package com.the_coffe_coders.fastestlap.source.constructor;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_TEAMS_COLLECTION;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorCallback;

public class FirebaseConstructorDataSource implements ConstructorDataSource {
    private static final String TAG = "FirebaseConstructorDataSource";
    private static FirebaseConstructorDataSource instance;
    private final FirebaseDatabase database;

    private FirebaseConstructorDataSource() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public static FirebaseConstructorDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseConstructorDataSource();
        }
        return instance;
    }

    @Override
    public void getConstructor(String constructorId, ConstructorCallback callback) {
        // Implementation for fetching constructor from Firebase
        DatabaseReference databaseReference = database.getReference(FIREBASE_TEAMS_COLLECTION).child(constructorId);
        databaseReference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Constructor constructor = snapshot.getValue(Constructor.class);
                            if (constructor != null) {
                                callback.onConstructorLoaded(constructor);
                            } else {
                                callback.onError(new Exception("Constructor data is null"));
                            }
                        } else {
                            callback.onError(new Exception("No constructor found"));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(new Exception("Firebase error: " + error.getMessage()));
                    }
                }
        );
    }
}
