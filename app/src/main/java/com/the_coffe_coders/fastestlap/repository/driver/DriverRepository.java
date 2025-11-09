package com.the_coffe_coders.fastestlap.repository.driver;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.source.driver.FirebaseDriverDataSource;
import com.the_coffe_coders.fastestlap.source.driver.JolpicaDriverDataSource;
import com.the_coffe_coders.fastestlap.source.driver.LocalDriverDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DriverRepository {
    private static final String TAG = "DriverRepository";
    public static DriverRepository instance;
    //Cache
    private final Map<String, MutableLiveData<Result>> driverCache;
    private final Map<String, Long> lastUpdateTimestamps;
    //Data sources
    final FirebaseDriverDataSource firebaseDriverDataSource;
    JolpicaDriverDataSource jolpicaDriverDataSource;
    final LocalDriverDataSource localDriverDataSource;
    AppRoomDatabase appRoomDatabase;
    private final Context context;

    //private final NetworkUtils networkLiveData;

    private DriverRepository(AppRoomDatabase appRoomDatabase, Context context) {
        driverCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        firebaseDriverDataSource = FirebaseDriverDataSource.getInstance();
        localDriverDataSource = LocalDriverDataSource.getInstance(appRoomDatabase);
        this.context = context.getApplicationContext();

        //networkLiveData = new NetworkUtils(context);
    }

    public static DriverRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new DriverRepository(appRoomDatabase, context);
        }
        return instance;
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    /*
    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    */

    public synchronized MutableLiveData<Result> getDriver(String driverId) {
        Log.d(TAG, "Fetching driver with ID: " + driverId);

        if (!driverCache.containsKey(driverId) || !lastUpdateTimestamps.containsKey(driverId) || lastUpdateTimestamps.get(driverId) == null) {
            driverCache.put(driverId, new MutableLiveData<>());
            loadDriver(driverId);
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(driverId) > 60000) {
            // Se i dati sono vecchi, prova a caricare nuovi dati solo se c'è connessione
            if (isNetworkAvailable()) {
                loadDriver(driverId);
            } else {
                Log.d(TAG, "No network connection, using cached data for driver: " + driverId);
                loadDriverFromLocal(driverId);
            }
        } else {
            Log.d(TAG, "Driver found in cache: " + driverId);
        }
        return driverCache.get(driverId);
    }

    private void loadDriverFromJolpica(String driverId) {
        jolpicaDriverDataSource.getDriver(driverId, new DriverCallback() {
            @Override
            public void onDriverLoaded(Driver driver) {
                if (driver != null) {
                    if (driverCache.containsKey(driverId)) {
                        Objects.requireNonNull(driverCache.get(driverId)).setValue(new Result.DriverSuccess(driver));
                    } else {
                        Log.e(TAG, "Driver not found in cache: " + driverId);
                        driverCache.put(driverId, new MutableLiveData<>(new Result.DriverSuccess(driver)));
                    }
                    lastUpdateTimestamps.put(driverId, System.currentTimeMillis());
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading driver from Jolpica: " + e.getMessage());
                Objects.requireNonNull(driverCache.get(driverId)).postValue(new Result.Error("Error loading driver from Jolpica: " + e.getMessage()));
            }
        });
    }

    public void loadDriverFromLocal(String driverId) {
        Log.d(TAG, "Loading driver from local database: " + driverId);
        localDriverDataSource.getDriver(driverId, new DriverCallback() {
            @Override
            public void onDriverLoaded(Driver driver) {
                if (driver != null) {
                    Log.d(TAG, "Driver loaded from local database: " + driverId);
                    driver.setDriverId(driverId);
                    driverCache.put(driverId, new MutableLiveData<>(new Result.DriverSuccess(driver)));
                    lastUpdateTimestamps.put(driverId, System.currentTimeMillis());
                    Objects.requireNonNull(driverCache.get(driverId)).postValue(new Result.DriverSuccess(driver));
                } else {
                    Log.e(TAG, "Driver not found in local database: " + driverId);
                    // Se il driver non è trovato neanche localmente, prova con Jolpica se c'è connessione
                    if (isNetworkAvailable() && jolpicaDriverDataSource != null) {
                        Log.d(TAG, "Trying Jolpica as fallback for driver: " + driverId);
                        loadDriverFromJolpica(driverId);
                    } else {
                        Objects.requireNonNull(driverCache.get(driverId)).postValue(
                                new Result.Error("Driver not found locally and no network connection available"));

                        throw new RuntimeException("Driver not found in local database: " + driverId);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading driver from local database: " + e.getMessage());
                Objects.requireNonNull(driverCache.get(driverId)).postValue(
                        new Result.Error("Error loading driver from local database: " + e.getMessage()));
            }
        });
    }

    private void loadDriver(String driverId) {
        // Controlla prima se c'è connessione di rete
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network connection available, loading from local database: " + driverId);
            driverCache.get(driverId).postValue(new Result.Loading("Loading driver from local database"));
            loadDriverFromLocal(driverId);
            return;
        }

        Log.d(TAG, "Network available, fetching driver from Firebase: " + driverId);
        driverCache.get(driverId).postValue(new Result.Loading("Fetching driver from remote"));

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
                    } else {
                        Log.e(TAG, "Driver not found in Firebase: " + driverId);
                        loadDriverFromLocal(driverId);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading driver from Firebase: " + e.getMessage());
                    Log.d(TAG, "Falling back to local database for driver: " + driverId);
                    loadDriverFromLocal(driverId);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception while loading driver from Firebase: " + e.getMessage());
            Log.d(TAG, "Falling back to local database for driver: " + driverId);
            loadDriverFromLocal(driverId);
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
        loadDriverFromLocal(driverId);
    }
}