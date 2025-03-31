package com.the_coffe_coders.fastestlap.source.driver_standings;

import com.the_coffe_coders.fastestlap.repository.standings.driver.DriverStandingCallback;
import com.the_coffe_coders.fastestlap.repository.standings.driver.DriverStandingsResponseCallback;

import lombok.Setter;

@Setter
public abstract class BaseDriverStandingsRemoteDataSource {
    public abstract void getDriversStandings(DriverStandingCallback callback);
}
