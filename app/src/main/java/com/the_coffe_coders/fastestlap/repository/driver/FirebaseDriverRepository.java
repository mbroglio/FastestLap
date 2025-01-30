package com.the_coffe_coders.fastestlap.repository.driver;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_DRIVERS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_NATIONS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_TEAMS_COLLECTION;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.FirebaseDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;

public class FirebaseDriverRepository implements IDriverRepository{
    private final FirebaseDatabase database;

    public FirebaseDriverRepository(FirebaseDatabase database) {
        this.database = database;
    }

    @Override
    public MutableLiveData<Result> fetchDrivers(long lastUpdate) {
        return null;
    }

    @Override
    public MutableLiveData<Result> fetchDriver(String driverId, long lastUpdate) {
        return null;
    }

    @Override
    public MutableLiveData<Result> fetchDriversStandings(long lastUpdate) {
        return null;
    }
}
