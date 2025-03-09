package com.the_coffe_coders.fastestlap.repository.nation;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_NATIONS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;

public class FirebaseNationRepository {
    private static FirebaseDatabase firebaseDatabase;
    private final String TAG = "FirebaseNationRepository";

    public FirebaseNationRepository() {
        firebaseDatabase = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    /*public MutableLiveData<Result> getNation(String nationId) {
        Log.i(TAG, "Fetching nation with ID: " + nationId);
        MutableLiveData<Result> nationMutableLiveData = new MutableLiveData<>();
        DatabaseReference nationReference = firebaseDatabase.getReference(FIREBASE_NATIONS_COLLECTION).child(nationId);
        Log.i(TAG, "Nation reference: " + nationReference);

        nationReference.get().addOnCompleteListener(nationTask -> {
            if (nationTask.isSuccessful()) {
                Nation nation = nationTask.getResult().getValue(Nation.class);
                nationMutableLiveData.postValue(new Result.NationSuccess(nation));
            }
        });

        return nationMutableLiveData;
    }*/

    public MutableLiveData<Result> getNation(String nationId) {
        Log.i(TAG, "Fetching nation with ID: " + nationId);
        MutableLiveData<Result> nationMutableLiveData = new MutableLiveData<>();
        DatabaseReference nationReference = firebaseDatabase.getReference(FIREBASE_NATIONS_COLLECTION).child(nationId);
        Log.i(TAG, "Nation reference: " + nationReference);

        nationReference.get().addOnCompleteListener(nationTask -> {
            if (nationTask.isSuccessful()) {
                try {
                    Nation nation = nationTask.getResult().getValue(Nation.class);
                    if (nation != null) {
                        nationMutableLiveData.postValue(new Result.NationSuccess(nation));
                    } else {
                        nationMutableLiveData.postValue(new Result.Error("Nation data is null"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deserializing nation data", e);
                    nationMutableLiveData.postValue(new Result.Error("Failed to parse nation data: " + e.getMessage()));
                }
            } else {
                Log.e(TAG, "Error fetching nation data", nationTask.getException());
                nationMutableLiveData.postValue(new Result.Error("Failed to fetch nation data: " +
                        (nationTask.getException() != null ? nationTask.getException().getMessage() : "Unknown error")));
            }
        });

        return nationMutableLiveData;
    }

}
