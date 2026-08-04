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
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;

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
    // Data Sources
    private final JolpicaDriverStandingsDataSource jolpicaDriverStandingsDataSource;
    private final LocalDriverStandingsDataSource localDriverStandingsDataSource;
    private final NetworkUtils networkLiveData;
    private final String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    // Prevents duplicate concurrent HTTP requests while a fetch is already in-flight.
    // Without this, callers that arrive before the first response lands would each fire
    // their own network request (since the timestamp is null until the callback completes).
    private boolean isFetchInFlight = false;

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
            driverStandingCache.put(cacheKey, new MutableLiveData<>());
            loadDriverStandingsCacheFirst(cacheKey);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(cacheKey);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) { // 5 min TTL
                if (isNetworkAvailable() && !isFetchInFlight) {
                    isFetchInFlight = true;
                    loadDriverStandingFromRemote(cacheKey, true);
                }
            } else {
                Log.d(TAG, "Driver standing found in cache");
            }
        }
        return driverStandingCache.get(cacheKey);
    }

    private void loadDriverStandingsCacheFirst(String cacheKey) {
        localDriverStandingsDataSource.getDriverStandings(new DriverStandingCallback() {
            @Override
            public void onDriverStandingsLoaded(DriverStandings driverStandings) {
                if (driverStandings != null && currentYear.equals(driverStandings.getSeason())) {
                    Log.d(TAG, "Driver standings loaded from local database (cache hit)");
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(driverStandingCache.get(cacheKey))
                            .postValue(new Result.DriverStandingsSuccess(driverStandings));

                    Long ts = lastUpdateTimestamps.get(cacheKey);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300_000L;
                    if (isNetworkAvailable() && !isFetchInFlight && isStale) {
                        isFetchInFlight = true;
                        loadDriverStandingFromRemote(cacheKey, true);
                    }
                } else {
                    Log.d(TAG, "Driver standings cache miss in local database");
                    if (isNetworkAvailable() && !isFetchInFlight) {
                        isFetchInFlight = true;
                        loadDriverStandingFromRemote(cacheKey, false);
                    } else if (!isFetchInFlight) {
                        Objects.requireNonNull(driverStandingCache.get(cacheKey))
                                .postValue(new Result.Error("Driver standing not found locally and no network connection"));
                    }
                }
            }

            @Override
            public void onDriverListLoaded(List<Driver> driverList) {
                Log.d(TAG, "Driver list only in local DB — fetching full standings from remote");
                if (isNetworkAvailable() && !isFetchInFlight) {
                    isFetchInFlight = true;
                    loadDriverStandingFromRemote(cacheKey, false);
                } else if (!isFetchInFlight) {
                    Objects.requireNonNull(driverStandingCache.get(cacheKey))
                            .postValue(new Result.Error("Driver standing not found locally and no network connection"));
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking local database for driver standings: " + e.getMessage());
                if (isNetworkAvailable() && !isFetchInFlight) {
                    isFetchInFlight = true;
                    loadDriverStandingFromRemote(cacheKey, false);
                }
            }
        });
    }

    private void loadDriverStandingFromRemote(String cacheKey, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            Objects.requireNonNull(driverStandingCache.get(cacheKey))
                    .postValue(new Result.Loading("Fetching driver standing from remote"));
        }
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
                    }
                }

                @Override
                public void onError(Exception e) {
                    isFetchInFlight = false;
                    Log.e(TAG, "Error loading driver standing from remote: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            isFetchInFlight = false;
            Log.e(TAG, "Error loading driver standing from remote: " + e.getMessage());
        }
    }
}