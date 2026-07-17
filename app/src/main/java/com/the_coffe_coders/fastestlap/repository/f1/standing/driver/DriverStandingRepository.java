package com.the_coffe_coders.fastestlap.repository.f1.standing.driver;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;
import com.the_coffe_coders.fastestlap.source.f1.standing.driver.JolpicaDriverStandingsDataSource;
import com.the_coffe_coders.fastestlap.source.f1.standing.driver.LocalDriverStandingsDataSource;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DriverStandingRepository {
    private static final String TAG = "DriverStandingRepository";
    private static DriverStandingRepository instance;

    // Cache
    private final Map<String, MutableLiveData<Result>> driverStandingCache;
    private final Map<String, Long> lastUpdateTimestamps;
    // Prevents duplicate concurrent HTTP requests while a fetch is already in-flight.
    // Without this, callers that arrive before the first response lands would each fire
    // their own network request (since the timestamp is null until the callback completes).
    private boolean isFetchInFlight = false;

    // Data Sources
    private final JolpicaDriverStandingsDataSource jolpicaDriverStandingsDataSource;
    private final LocalDriverStandingsDataSource localDriverStandingsDataSource;

    private final NetworkUtils networkLiveData;

    private final String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

    private DriverStandingRepository(AppRoomDatabase appRoomDatabase, Context context) {
        driverStandingCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        this.jolpicaDriverStandingsDataSource = JolpicaDriverStandingsDataSource.getInstance();
        this.localDriverStandingsDataSource = LocalDriverStandingsDataSource.getInstance(appRoomDatabase);
        this.networkLiveData = new NetworkUtils(context);
    }

    public static synchronized DriverStandingRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new DriverStandingRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    }

    public synchronized MutableLiveData<Result> getDriverStandings() {
        Log.d(TAG, "Fetching driver standing");
        String cacheKey = "driverStanding";

        if (!driverStandingCache.containsKey(cacheKey)) {
            // First call ever: create the LiveData and start the fetch.
            driverStandingCache.put(cacheKey, new MutableLiveData<>());
            lastUpdateTimestamps.remove(cacheKey);
        }

        if (!lastUpdateTimestamps.containsKey(cacheKey) || lastUpdateTimestamps.get(cacheKey) == null) {
            // No successful result yet. Only start a fetch if one isn't already in-flight.
            if (!isFetchInFlight) {
                isFetchInFlight = true;
                if (isNetworkAvailable()) {
                    loadDriverStanding();
                } else {
                    fetchFromLocal(cacheKey);
                }
            } else {
                Log.d(TAG, "Driver standing fetch already in-flight, returning shared LiveData");
            }
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(cacheKey) > 60000) {
            // Cache is stale — refresh.
            if (!isFetchInFlight) {
                isFetchInFlight = true;
                if (isNetworkAvailable()) {
                    loadDriverStanding();
                } else {
                    fetchFromLocal(cacheKey);
                }
            }
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
                public void onDriverStandingsLoaded(DriverStandings driverStandings) {
                    isFetchInFlight = false;
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
                public void onDriverListLoaded(List<Driver> driverList) {
                    isFetchInFlight = false;
                    if (driverList != null) {
                        localDriverStandingsDataSource.insertDriverList(driverList);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(driverStandingCache.get(cacheKey))
                                .postValue(new Result.DriversSuccess(driverList));
                    } else {
                        Log.e(TAG, "Driver list not found");
                        fetchFromLocal(cacheKey);
                    }
                }

                @Override
                public void onError(Exception e) {
                    isFetchInFlight = false;
                    Log.e(TAG, "Error loading driver standing: " + e.getMessage());
                    fetchFromLocal(cacheKey);
                }


            });
        } catch (Exception e) {
            isFetchInFlight = false;
            Log.e(TAG, "Error loading driver standing: " + e.getMessage());
            fetchFromLocal(cacheKey);
        }
    }

    private void fetchFromLocal(String cacheKey) {
        localDriverStandingsDataSource.getDriverStandings(new DriverStandingCallback() {
            @Override
            public void onDriverStandingsLoaded(DriverStandings driverStandings) {
                if (driverStandings != null && driverStandings.getSeason().equals(currentYear)) {
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
            public void onDriverListLoaded(List<Driver> driverList) {
                if (driverList != null) {
                    driverStandingCache.put(cacheKey, new MutableLiveData<>(
                            new Result.DriversSuccess(driverList)));
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(driverStandingCache.get(cacheKey))
                            .postValue(new Result.DriversSuccess(driverList));
                } else {
                    Log.e(TAG, "Driver list not found in local database");
                    Objects.requireNonNull(driverStandingCache.get(cacheKey))
                            .postValue(new Result.Error("Driver list not found"));
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