package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.repository.driver.DriverCallback;

import okhttp3.Callback;

public class JolpicaDriverDataSource implements DriverDataSource {
    private static JolpicaDriverDataSource instance;
    private static final String TAG = "JolpicaDriverDataSource";
    private JolpicaDriverDataSource() {
        // Empty constructor
    }

    public static synchronized JolpicaDriverDataSource getInstance() {
        if (instance == null) {
            instance = new JolpicaDriverDataSource();
        }
        return instance;
    }

    @Override
    public void getDriver(String driverId, DriverCallback callback) {

    }

    @Override
    public void getDrivers(Callback callback) {

    }
}
