package com.the_coffe_coders.fastestlap.repository.driver;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.source.driver.FirebaseDriverDataSource;
import com.the_coffe_coders.fastestlap.source.driver.JolpicaDriverDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DriverRepository {
    private static final String TAG = "DriverRepository";

    //Data sources
    FirebaseDriverDataSource firebaseDriverDataSource;
    JolpicaDriverDataSource jolpicaDriverDataSource;

    //Cache
    private final Map<String, MutableLiveData<Result>> driverCache;
    private final Map<String, Long> lastUpdateTimestamps;

    public static DriverRepository instance;

    public static DriverRepository getInstance() {
        if(instance == null) {
            instance = new DriverRepository();
        }
        return instance;
    }

    private DriverRepository() {
        driverCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        firebaseDriverDataSource = FirebaseDriverDataSource.getInstance();
    }

    public synchronized MutableLiveData<Result> getDriver(String driverId) {
        Log.d(TAG, "Fetching driver with ID: " + driverId);
        if(!driverCache.containsKey(driverId) || !lastUpdateTimestamps.containsKey(driverId) || lastUpdateTimestamps.get(driverId) == null) {
            driverCache.put(driverId, new MutableLiveData<>());
            loadDriver(driverId);
        }else if(System.currentTimeMillis() - lastUpdateTimestamps.get(driverId) > 60000) {
            loadDriver(driverId);
        } else {
            Log.d(TAG, "Driver found in cache: " + driverId);
        }
        return driverCache.get(driverId);
    }

    private void loadDriverFromJolpica(String driverId) {
        jolpicaDriverDataSource.getDriver(driverId, new DriverCallback() {
            @Override
            public void onDriverLoaded(Driver driver) {
                if(driver!=null) {
                    if(driverCache.containsKey(driverId)) {
                        Objects.requireNonNull(driverCache.get(driverId)).setValue(new Result.DriverSuccess(driver));
                    }else {
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

    private void loadDriver(String driverId) {
        driverCache.get(driverId).postValue(new Result.Loading("Fetching driver from remote"));
        try {
            firebaseDriverDataSource.getDriver(driverId, new DriverCallback() {
                @Override
                public void onDriverLoaded(Driver driver) {
                    if(driver!=null) {
                        driver.setDriverId(driverId);
                        lastUpdateTimestamps.put(driverId, System.currentTimeMillis());
                        Objects.requireNonNull(driverCache.get(driverId)).postValue(new Result.DriverSuccess(driver));
                    }else {
                        Log.e(TAG, "Driver not found: " + driverId);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading driver: " + e.getMessage());
                    //fetch driver local database
                }
            });

        }catch (Exception e) {
            Log.e(TAG, "Error loading driver: " + e.getMessage());
            //fetch driver local database
        }
    }
}
