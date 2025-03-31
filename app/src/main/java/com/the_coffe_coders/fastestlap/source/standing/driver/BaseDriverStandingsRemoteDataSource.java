package com.the_coffe_coders.fastestlap.source.standing.driver;

import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingCallback;

import lombok.Setter;

@Setter
public abstract class BaseDriverStandingsRemoteDataSource {
    public abstract void getDriversStandings(DriverStandingCallback callback);
}
