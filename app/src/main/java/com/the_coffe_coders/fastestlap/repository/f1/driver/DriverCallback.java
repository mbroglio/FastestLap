package com.the_coffe_coders.fastestlap.repository.f1.driver;

import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;

public interface DriverCallback {
    void onDriverLoaded(Driver driver);

    void onError(Exception e);
}
