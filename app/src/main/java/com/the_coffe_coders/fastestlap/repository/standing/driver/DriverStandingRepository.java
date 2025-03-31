package com.the_coffe_coders.fastestlap.repository.standing.driver;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.source.standing.driver.BaseDriverStandingsRemoteDataSource;

public class DriverStandingRepository {
    private final BaseDriverStandingsRemoteDataSource remoteDataSource;

    private static DriverStandingRepository instance;

    private final MutableLiveData<Result> driverStandingResult;

    private Long lastUpdate;

    private DriverStandingRepository(BaseDriverStandingsRemoteDataSource remoteDataSource) {
        this.remoteDataSource = remoteDataSource;
        this.lastUpdate = null;
        this.driverStandingResult = new MutableLiveData<>();
    }

    public static synchronized DriverStandingRepository getInstance(BaseDriverStandingsRemoteDataSource remoteDataSource) {
        if (instance == null) {
            instance = new DriverStandingRepository(remoteDataSource);
        }
        return instance;
    }

    public MutableLiveData<Result> fetchDriverStanding() {
        long FRESH_TIMEOUT = 60000;
        if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > FRESH_TIMEOUT) {
            Log.i("DriverStandingRepository", "Fetching driver standing from remote");
            loadDriverStanding();
        }
        return driverStandingResult;
    }

    private void loadDriverStanding() {
        try {
            remoteDataSource.getDriversStandings(new DriverStandingCallback(){
                @Override
                public void onSuccess(DriverStandings driverStanding) {
                    Log.i("DriverStandingRepository", "Driver standing fetched from remote" + driverStanding);
                    lastUpdate = System.currentTimeMillis();
                    driverStandingResult.postValue(new Result.DriverStandingsSuccess(driverStanding));
                }
                @Override
                public void onFailure(Exception exception) {
                    driverStandingResult.postValue(new Result.Error(exception.getMessage()));
                    // fetch local db instead
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
