package com.the_coffe_coders.fastestlap.source.driver_standings;

import com.the_coffe_coders.fastestlap.repository.standings.DriverStandingsResponseCallback;

import lombok.Setter;

@Setter
public abstract class BaseDriverStandingsRemoteDataSource {
    protected DriverStandingsResponseCallback driverCallback;

    public abstract void getDriversStandings();
}
