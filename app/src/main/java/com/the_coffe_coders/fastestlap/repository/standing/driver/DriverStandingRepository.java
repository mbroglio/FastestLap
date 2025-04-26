package com.the_coffe_coders.fastestlap.repository.standing.driver;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.source.standing.driver.BaseDriverStandingsLocalDataSource;
import com.the_coffe_coders.fastestlap.source.standing.driver.BaseDriverStandingsRemoteDataSource;

public class DriverStandingRepository {
    private static final String TAG = "DriverStandingRepository";
    private static DriverStandingRepository instance;
    private final BaseDriverStandingsRemoteDataSource remoteDataSource;
    private final BaseDriverStandingsLocalDataSource localDataSource;
    private final MutableLiveData<Result> driverStandingResult;

    private Long lastUpdate;

    private DriverStandingRepository(BaseDriverStandingsRemoteDataSource remoteDataSource, BaseDriverStandingsLocalDataSource localDataSource) {
        this.remoteDataSource = remoteDataSource;
        this.localDataSource = localDataSource;
        this.lastUpdate = null;
        this.driverStandingResult = new MutableLiveData<>();
    }

    public static synchronized DriverStandingRepository getInstance(BaseDriverStandingsRemoteDataSource remoteDataSource, BaseDriverStandingsLocalDataSource localDataSource) {
        if (instance == null) {
            instance = new DriverStandingRepository(remoteDataSource, localDataSource);
        }
        return instance;
    }

    public synchronized MutableLiveData<Result> fetchDriverStanding() {
        long FRESH_TIMEOUT = 60000;
        if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > FRESH_TIMEOUT) {
            Log.i("DriverStandingRepository", "Fetching driver standing from remote");
            driverStandingResult.postValue(new Result.Loading("Fetching driver standing from remote"));
            loadDriverStanding();
        }

        return driverStandingResult;
    }

    private void loadDriverStanding() {
        driverStandingResult.postValue(new Result.Loading("Fetching driver standing from remote"));
        try {
            remoteDataSource.getDriversStandings(new DriverStandingCallback() {
                @Override
                public void onSuccess(DriverStandings driverStanding) {
                    Log.i("DriverStandingRepository", "Driver standing fetched from remote" + driverStanding);
                    lastUpdate = System.currentTimeMillis();
                    driverStandingResult.setValue(new Result.DriverStandingsSuccess(driverStanding));
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.e("DriverStandingRepository", "Error fetching driver standing from remote", exception);
                    loadDriverStandingFromLocal();
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadDriverStandingFromLocal() {
        Log.i("DriverStandingRepository", "Fetching driver standing from local");

        localDataSource.getDriversStandings(new DriverStandingCallback() {
            @Override
            public void onSuccess(DriverStandings driverStanding) {
                driverStandingResult.postValue(new Result.DriverStandingsSuccess(driverStanding));
            }

            @Override
            public void onFailure(Exception exception) {
                driverStandingResult.postValue(new Result.Error(exception.getMessage()));
            }
        });
    }
}
