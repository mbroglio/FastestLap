package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.repository.driver.DriverCallback;

import okhttp3.Callback;

public class DriverLocalDataSource implements DriverDataSource {
    private static DriverLocalDataSource instance;
    private static final String TAG = "DriverLocalDataSource";
    private DriverLocalDataSource() {
        // Empty constructor
    }
    public static synchronized DriverLocalDataSource getInstance() {
        if (instance == null) {
            instance = new DriverLocalDataSource();
        }
        return instance;
    }

    @Override
    public void getDriver(String driverId, DriverCallback callback) {

    }

    @Override
    public void getDrivers(Callback callback) {

    }

    public void insertDriver(Driver driver) {

    }
}
