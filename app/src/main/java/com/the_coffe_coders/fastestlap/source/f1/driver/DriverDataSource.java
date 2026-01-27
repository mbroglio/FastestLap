package com.the_coffe_coders.fastestlap.source.f1.driver;

import com.the_coffe_coders.fastestlap.repository.f1.driver.DriverCallback;

public interface DriverDataSource {
    void getDriver(String driverId, DriverCallback callback);
}