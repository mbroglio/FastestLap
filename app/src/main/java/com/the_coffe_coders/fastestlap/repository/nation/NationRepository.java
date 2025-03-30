package com.the_coffe_coders.fastestlap.repository.nation;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_NATIONS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NationRepository {
    private static FirebaseDatabase firebaseDatabase;
    private final String TAG = "NationRepository";

    Map<String, MutableLiveData<Result>> nationCache;
    Map<String, Long> lastUpdateTimestamps;

    public static NationRepository instance;

    public static NationRepository getInstance() {
        if (instance == null) {
            instance = new NationRepository();
        }
        return instance;
    }

    public NationRepository() {
        firebaseDatabase = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
        nationCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
    }

    public MutableLiveData<Result> getNation(String nationId) {
        Log.i(TAG, "Fetching nation with ID: " + nationId);
        if (!nationCache.containsKey(nationId) || !lastUpdateTimestamps.containsKey(nationId) || lastUpdateTimestamps.get(nationId) == null) {
            Log.i(TAG, "Nation not found in cache: " + nationId);
            nationCache.put(nationId, new MutableLiveData<>());
            loadNation(nationId);
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(nationId) > 60000) {
            Log.i(TAG, "Nation found in cache but outdated: " + nationId);
            loadNation(nationId);
        } else {
            Log.i(TAG, "Nation found in cache: " + nationId);
        }
        return nationCache.get(nationId);
    }

    public void loadNation(String nationId) {
        DatabaseReference nationReference = firebaseDatabase.getReference(FIREBASE_NATIONS_COLLECTION).child(nationId);
        nationReference.get().addOnCompleteListener(nationTask -> {
            if (nationTask.isSuccessful()) {
                try {
                    Nation nation = nationTask.getResult().getValue(Nation.class);
                    if (nation != null) {
                        nation.setNationId(nationId);
                        if (nationCache.containsKey(nationId)) {
                            Objects.requireNonNull(nationCache.get(nationId)).postValue(new Result.NationSuccess(nation));
                            lastUpdateTimestamps.put(nationId, System.currentTimeMillis());
                        } else {
                            nationCache.put(nationId, new MutableLiveData<>(new Result.NationSuccess(nation)));
                            lastUpdateTimestamps.put(nationId, System.currentTimeMillis());
                        }
                    } else {
                        Objects.requireNonNull(nationCache.get(nationId)).postValue(new Result.Error("Nation not found"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deserializing nation data", e);
                    Objects.requireNonNull(nationCache.get(nationId)).postValue(new Result.Error("Failed to parse nation data: " + e.getMessage()));
                }
            } else {
                Log.e(TAG, "Error fetching nation data", nationTask.getException());
                Objects.requireNonNull(nationCache.get(nationId)).postValue(new Result.Error("Failed to fetch nation data: " +
                        (nationTask.getException() != null ? nationTask.getException().getMessage() : "Unknown error")));
            }
        });
    }

}
