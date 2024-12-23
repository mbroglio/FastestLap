package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.repository.driver.DriverResponseCallback;

public abstract class BaseDriverLocalDataSource {
    protected DriverResponseCallback driverCallback;

    public void setDriverCallback(DriverResponseCallback driverCallback) {
        this.driverCallback = driverCallback;
    }

    public abstract void getDrivers();
}