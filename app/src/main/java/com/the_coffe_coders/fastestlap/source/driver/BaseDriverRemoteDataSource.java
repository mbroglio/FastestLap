package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.repository.driver.DriverStandingsResponseCallback;

public abstract class BaseDriverRemoteDataSource {
    protected DriverStandingsResponseCallback driverCallback;

    public void setDriverCallback(DriverStandingsResponseCallback driverCallback) {
        this.driverCallback = driverCallback;
    }

    public abstract void getDrivers();

    public abstract void getDriversStandings();
}
