package com.the_coffe_coders.fastestlap.source.nation;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_NATIONS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.repository.nation.NationCallback;

public class FirebaseNationDataSource implements NationDataSource {
    private static final String TAG = "FirebaseNationDataSource";
    private static FirebaseNationDataSource instance;
    private final FirebaseDatabase database;

    public FirebaseNationDataSource() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public static synchronized FirebaseNationDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseNationDataSource();
        }
        return instance;
    }

    @Override
    public void getNation(String nationId, NationCallback callback) {
        Log.i(TAG, "Fetching nation from Firebase with ID: " + nationId);
        DatabaseReference databaseReference = database.getReference(FIREBASE_NATIONS_COLLECTION).child(nationId);

        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    Nation nation = snapshot.getValue(Nation.class);
                    if (nation != null) {
                        nation.setNationId(nationId);
                        Log.i(TAG, "Successfully retrieved nation from Firebase: " + nation);
                        callback.onNationLoaded(nation);
                    } else {
                        Log.e(TAG, "Nation data is null for ID: " + nationId);
                        callback.onError(new Exception("Nation data is null"));
                    }
                } else {
                    Log.e(TAG, "No nation found for ID: " + nationId);
                    callback.onError(new Exception("No nation found"));
                }
            } else {
                Exception error = task.getException() != null ? task.getException() : new Exception("Firebase request failed");
                Log.e(TAG, "Firebase request cancelled/failed: " + error.getMessage());
                callback.onError(error);
            }
        });
    }
}