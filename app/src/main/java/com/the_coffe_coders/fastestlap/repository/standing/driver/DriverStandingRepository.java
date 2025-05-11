package com.the_coffe_coders.fastestlap.repository.standing.driver;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.source.standing.driver.JolpicaDriverStandingsDataSource;
import com.the_coffe_coders.fastestlap.source.standing.driver.LocalDriverStandingsDataSource;

import java.util.HashMap;
import java.util.Map;

public class DriverStandingRepository {
    private static final String TAG = "DriverStandingRepository";
    private static DriverStandingRepository instance;

    // Cache
    private final Map<String, MutableLiveData<Result>> driverStandingCache;
    private final Map<String, Long> lastUpdateTimestamps;

    // Data Sources
    private final JolpicaDriverStandingsDataSource jolpicaDriverStandingsDataSource;
    private final LocalDriverStandingsDataSource localDriverStandingsDataSource;

    private DriverStandingRepository(AppRoomDatabase appRoomDatabase) {
        driverStandingCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        this.jolpicaDriverStandingsDataSource = JolpicaDriverStandingsDataSource.getInstance();
        this.localDriverStandingsDataSource = LocalDriverStandingsDataSource.getInstance(appRoomDatabase);
    }

    public static synchronized DriverStandingRepository getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new DriverStandingRepository(appRoomDatabase);
        }
        return instance;
    }

    public synchronized MutableLiveData<Result> getDriverStandings() {
        Log.d(TAG, "Fetching driver standing");
        String cacheKey = "driverStanding";

        if(!driverStandingCache.containsKey(cacheKey) || !lastUpdateTimestamps.containsKey(cacheKey) || lastUpdateTimestamps.get(cacheKey) == null) {
            driverStandingCache.put(cacheKey, new MutableLiveData<>());
            loadDriverStanding();
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(cacheKey) > 60000) {
            loadDriverStanding();
        } else {
            Log.d(TAG, "Driver standing found in cache");

        }

        return driverStandingCache.get(cacheKey);
    }

    private void loadDriverStanding() {
        driverStandingResult.postValue(new Result.Loading("Fetching driver standing from remote"));
        try {
            remoteDataSource.getDriversStandings(new DriverStandingCallback(){
                @Override
                public void onDriverLoaded(DriverStandings driverStanding) {
                    Log.i("DriverStandingRepository", "Driver standing fetched from remote" + driverStanding);
                    lastUpdate = System.currentTimeMillis();
                    driverStandingResult.setValue(new Result.DriverStandingsSuccess(driverStanding));
                }
                @Override
                public void onError(Exception exception) {
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

        localDataSource.getDriversStandings(new DriverStandingCallback(){
            @Override
            public void onDriverLoaded(DriverStandings driverStanding) {
                driverStandingResult.postValue(new Result.DriverStandingsSuccess(driverStanding));
            }

            @Override
            public void onError(Exception exception) {
                driverStandingResult.postValue(new Result.Error(exception.getMessage()));
            }
        });
    }
}
