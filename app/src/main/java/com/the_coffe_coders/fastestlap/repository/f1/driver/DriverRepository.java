package com.the_coffe_coders.fastestlap.repository.f1.driver;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.source.f1.driver.FirebaseDriverDataSource;
import com.the_coffe_coders.fastestlap.source.f1.driver.JolpicaDriverDataSource;
import com.the_coffe_coders.fastestlap.source.f1.driver.LocalDriverDataSource;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DriverRepository {
    private static final String TAG = "DriverRepository";
    public static DriverRepository instance;
    //Data sources
    final FirebaseDriverDataSource firebaseDriverDataSource;
    final LocalDriverDataSource localDriverDataSource;
    //Cache
    private final Map<String, MutableLiveData<Result>> driverCache;
    private final Map<String, Long> lastUpdateTimestamps;
    private final NetworkUtils networkLiveData;
    JolpicaDriverDataSource jolpicaDriverDataSource;

    private DriverRepository(AppRoomDatabase appRoomDatabase, Context context) {
        driverCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        firebaseDriverDataSource = FirebaseDriverDataSource.getInstance();
        localDriverDataSource = LocalDriverDataSource.getInstance(appRoomDatabase);
        networkLiveData = new NetworkUtils(context);
    }

    public static DriverRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new DriverRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    }

    public synchronized MutableLiveData<Result> getDriver(String driverId) {
        Log.d(TAG, "Fetching driver with ID: " + driverId);

        if (!driverCache.containsKey(driverId)) {
            driverCache.put(driverId, new MutableLiveData<>());
            loadDriverCacheFirst(driverId);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(driverId);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) { // 5 mins cache TTL
                if (isNetworkAvailable()) {
                    loadDriverFromRemote(driverId, true);
                }
            } else {
                Log.d(TAG, "Driver found in cache: " + driverId);
            }
        }
        return driverCache.get(driverId);
    }

    private void loadDriverCacheFirst(String driverId) {
        // Step 1: Query local Room database first
        localDriverDataSource.getDriver(driverId, new DriverCallback() {
            @Override
            public void onDriverLoaded(Driver driver) {
                if (driver != null) {
                    Log.d(TAG, "Driver loaded from local database (cache hit): " + driverId);
                    driver.setDriverId(driverId);
                    lastUpdateTimestamps.put(driverId, System.currentTimeMillis());
                    Objects.requireNonNull(driverCache.get(driverId)).postValue(new Result.DriverSuccess(driver));

                    // Step 2: Only refresh from remote if the cached data is actually stale.
                    // Without this TTL guard, Firebase fires on every launch even when the
                    // local data is fresh, causing the LiveData to re-emit and triggering
                    // redundant card rebuilds in the UI.
                    Long ts = lastUpdateTimestamps.get(driverId);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300_000L;
                    if (isNetworkAvailable() && isStale) {
                        loadDriverFromRemote(driverId, true);
                    } else {
                        Log.d(TAG, "Driver cache still fresh, skipping remote refresh: " + driverId);
                    }
                } else {
                    Log.d(TAG, "Driver cache miss in local database: " + driverId);
                    // Step 3: Fetch from remote if not present locally
                    if (isNetworkAvailable()) {
                        loadDriverFromRemote(driverId, false);
                    } else {
                        Objects.requireNonNull(driverCache.get(driverId)).postValue(
                                new Result.Error("Driver not found locally and no network connection available"));
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking local database for driver: " + e.getMessage());
                if (isNetworkAvailable()) {
                    loadDriverFromRemote(driverId, false);
                }
            }
        });
    }

    private void loadDriverFromRemote(String driverId, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            driverCache.get(driverId).postValue(new Result.Loading("Fetching driver from remote"));
        }

        try {
            firebaseDriverDataSource.getDriver(driverId, new DriverCallback() {
                @Override
                public void onDriverLoaded(Driver driver) {
                    if (driver != null) {
                        Log.d(TAG, "Driver loaded from Firebase: " + driverId);
                        driver.setDriverId(driverId);
                        localDriverDataSource.insertDriver(driver);
                        lastUpdateTimestamps.put(driverId, System.currentTimeMillis());
                        Objects.requireNonNull(driverCache.get(driverId)).postValue(new Result.DriverSuccess(driver));
                    } else if (!isBackgroundRefresh) {
                        Log.e(TAG, "Driver not found in Firebase: " + driverId);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading driver from Firebase: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception while loading driver from Firebase: " + e.getMessage());
        }
    }

    /**
     * Forza il caricamento dal database locale, utile per test o situazioni specifiche
     */
    public void forceLoadFromLocal(String driverId) {
        Log.d(TAG, "Force loading driver from local database: " + driverId);
        if (!driverCache.containsKey(driverId)) {
            driverCache.put(driverId, new MutableLiveData<>());
        }
        driverCache.get(driverId).postValue(new Result.Loading("Loading driver from local database"));
        loadDriverCacheFirst(driverId);
    }
}