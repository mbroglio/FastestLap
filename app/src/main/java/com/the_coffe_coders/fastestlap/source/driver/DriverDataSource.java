package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.repository.driver.DriverCallback;

public interface DriverDataSource {
    void getDriver(String driverId, DriverCallback callback);
}