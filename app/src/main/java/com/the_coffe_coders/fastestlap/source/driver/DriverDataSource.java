package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.repository.driver.DriverCallback;

import okhttp3.Callback;

public interface DriverDataSource {
    void getDriver(String driverId, DriverCallback callback);
}
