package com.the_coffe_coders.fastestlap.source.standing.driver;

import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;

import lombok.Setter;

@Setter
public abstract class BaseDriverStandingsLocalDataSource {
    public abstract void getDriversStandings();

    public abstract void insertDriversStandings(DriverStandings driverStandings);
}