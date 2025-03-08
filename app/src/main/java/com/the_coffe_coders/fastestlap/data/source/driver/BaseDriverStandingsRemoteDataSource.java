package com.the_coffe_coders.fastestlap.data.source.driver;

import com.the_coffe_coders.fastestlap.data.repository.standings.DriverStandingsResponseCallback;

import lombok.Setter;

@Setter
public abstract class BaseDriverStandingsRemoteDataSource {
    protected DriverStandingsResponseCallback driverCallback;
    public abstract void getDriversStandings();
}
