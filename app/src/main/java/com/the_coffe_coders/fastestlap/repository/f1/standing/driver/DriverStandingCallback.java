package com.the_coffe_coders.fastestlap.repository.f1.standing.driver;

import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;

import java.util.List;

public interface DriverStandingCallback {
    void onDriverStandingsLoaded(DriverStandings driverStanding);

    void onDriverListLoaded(List<Driver> driverList);

    void onError(Exception exception);
}
