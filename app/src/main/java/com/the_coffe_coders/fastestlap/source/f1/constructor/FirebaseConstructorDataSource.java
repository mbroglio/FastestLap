package com.the_coffe_coders.fastestlap.source.f1.constructor;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_TEAMS_COLLECTION;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.repository.f1.constructor.ConstructorCallback;

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
        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    Constructor constructor = snapshot.getValue(Constructor.class);
                    if (constructor != null) {
                        constructor.setConstructorId(constructorId);
                        callback.onConstructorLoaded(constructor);
                    } else {
                        callback.onError(new Exception("Constructor data is null"));
                    }
                } else {
                    callback.onError(new Exception("No constructor found"));
                }
            } else {
                Exception error = task.getException() != null ? task.getException() : new Exception("Firebase request failed");
                callback.onError(error);
            }
        });
    }
}
