package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.repository.driver.DriverResponseCallback;

import java.util.List;

public abstract class BaseDriverLocalDataSource {
    protected DriverResponseCallback driverCallback;

    public void setDriverCallback(DriverResponseCallback driverCallback) {
        this.driverCallback = driverCallback;
    }

    public abstract void getDrivers();

    public abstract void getDriversStandings();

    public abstract void getFavoriteDriver();

    public abstract void updateDriver(Driver driver);

    public abstract void insertDrivers(List<Driver> driverList);

    public abstract void insertDriversStandings(DriverStandings driverStandings);
}