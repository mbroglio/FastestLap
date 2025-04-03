package com.the_coffe_coders.fastestlap.repository.driver;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;

public interface DriverCallback {
    void onDriverLoaded(Driver driver);
    void onError(Exception e);

}
