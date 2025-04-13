package com.the_coffe_coders.fastestlap.source.standing.driver;

import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingCallback;

import lombok.Setter;

@Setter
public abstract class BaseDriverStandingsLocalDataSource {
    public abstract void getDriversStandings(DriverStandingCallback callback);

    public abstract void insertDriversStandings(DriverStandings driverStandings);
}