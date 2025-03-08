package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.repository.driver.DriverStandingsResponseCallback;

import lombok.Setter;

@Setter
public abstract class BaseDriverStandingsRemoteDataSource {
    protected DriverStandingsResponseCallback driverCallback;
    public abstract void getDriversStandings();
}
