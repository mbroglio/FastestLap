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
import java.util.Objects;

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

        if (!driverStandingCache.containsKey(cacheKey) ||
                !lastUpdateTimestamps.containsKey(cacheKey) ||
                lastUpdateTimestamps.get(cacheKey) == null) {
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
        String cacheKey = "driverStanding";
        Objects.requireNonNull(driverStandingCache.get(cacheKey)).postValue(new Result.Loading("Fetching driver standing from remote"));
        try {
            jolpicaDriverStandingsDataSource.getDriverStandings(new DriverStandingCallback() {
                @Override
                public void onDriverLoaded(DriverStandings driverStandings) {
                    if (driverStandings != null) {
                        localDriverStandingsDataSource.insertDriverStandings(driverStandings);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(driverStandingCache.get(cacheKey))
                                .postValue(new Result.DriverStandingsSuccess(driverStandings));
                    } else {
                        Log.e(TAG, "Driver standing not found");
                        fetchFromLocal(cacheKey);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading driver standing: " + e.getMessage());
                    fetchFromLocal(cacheKey);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading driver standing: " + e.getMessage());
            fetchFromLocal(cacheKey);
        }
    }

    private void fetchFromLocal(String cacheKey) {
        localDriverStandingsDataSource.getDriverStandings(new DriverStandingCallback() {
            @Override
            public void onDriverLoaded(DriverStandings driverStandings) {
                if (driverStandings != null) {
                    driverStandingCache.put(cacheKey, new MutableLiveData<>(
                            new Result.DriverStandingsSuccess(driverStandings)));
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(driverStandingCache.get(cacheKey))
                            .postValue(new Result.DriverStandingsSuccess(driverStandings));
                } else {
                    Log.e(TAG, "Driver standing not found in local database");
                    Objects.requireNonNull(driverStandingCache.get(cacheKey))
                            .postValue(new Result.Error("Driver standing not found"));
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading driver standing from local database: " + e.getMessage());
                Objects.requireNonNull(driverStandingCache.get(cacheKey))
                        .postValue(new Result.Error("Error loading driver standing: " + e.getMessage()));
            }
        });
    }
}