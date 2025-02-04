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
    private final MutableLiveData<Result> nationMutableLiveData;

    public FirebaseNationRepository() {
        nationMutableLiveData = new MutableLiveData<>();
        firebaseDatabase = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public MutableLiveData<Result> getNation(String nationId) {
        DatabaseReference nationReference = firebaseDatabase.getReference(FIREBASE_NATIONS_COLLECTION).child(nationId);
        Log.i(TAG, "Nation reference: " + nationReference);

        nationReference.get().addOnCompleteListener(nationTask -> {
            if (nationTask.isSuccessful()) {
                Nation nation = nationTask.getResult().getValue(Nation.class);
                Log.i("DriverBioActivity", "Nation data: " + nation);

                nationMutableLiveData.postValue(new Result.NationSuccess(nation));
            }
        });

        return nationMutableLiveData;
    }

}
