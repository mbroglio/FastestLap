package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.repository.driver.DriverStandingsResponseCallback;

import lombok.Setter;

@Setter
public abstract class BaseDriverStandingsLocalDataSource {
    protected DriverStandingsResponseCallback driverCallback;
    public abstract void getDriversStandings();
    public abstract void insertDriversStandings(DriverStandings driverStandings);
}