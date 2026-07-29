package com.the_coffe_coders.fastestlap.source.f1.driver;

import com.the_coffe_coders.fastestlap.repository.f1.driver.DriverCallback;

public class JolpicaDriverDataSource implements DriverDataSource {
    private static final String TAG = "JolpicaDriverDataSource";
    private static JolpicaDriverDataSource instance;

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
}
