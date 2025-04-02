package com.the_coffe_coders.fastestlap.repository.standing.constructor;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.source.standing.constructor.BaseConstructorStandingsRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.standing.constructor.ConstructorStandingsRemoteDataSource;

public class ConstructorStandingRepository {
    private static ConstructorStandingRepository instance;
    private final MutableLiveData<Result> constructorStandingResult;

    private final BaseConstructorStandingsRemoteDataSource remoteDataSource;
    private Long lastUpdate;
    private final long FRESH_TIMEOUT = 60000;

    private ConstructorStandingRepository() {
        constructorStandingResult = new MutableLiveData<>();
        remoteDataSource = new ConstructorStandingsRemoteDataSource();
        lastUpdate = FRESH_TIMEOUT;
    }

    public static ConstructorStandingRepository getInstance() {
        if (instance == null) {
            instance = new ConstructorStandingRepository();
        }
        return instance;
    }

    public synchronized MutableLiveData<Result> fetchConstructorStanding() {
        if (System.currentTimeMillis() - lastUpdate > FRESH_TIMEOUT) {
            loadConstructorStanding();
        }
        return constructorStandingResult;
    }

    public void loadConstructorStanding() {
        constructorStandingResult.postValue(new Result.Loading("Fetching constructor standing from remote"));
        try {
            remoteDataSource.getConstructorStandings(new ConstructorStandingCallback(){
                @Override
                public void onConstructorLoaded(ConstructorStandings constructorStandings) {
                    Log.i("DriverStandingRepository", "Driver standing fetched from remote" + constructorStandings);
                    lastUpdate = System.currentTimeMillis();
                    constructorStandingResult.postValue(new Result.ConstructorStandingsSuccess(constructorStandings));
                }
                @Override
                public void onFailure(Exception exception) {
                    constructorStandingResult.postValue(new Result.Error(exception.getMessage()));
                    // fetch local db instead
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
