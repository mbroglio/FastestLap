package com.the_coffe_coders.fastestlap.source.standing.driver;

import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingCallback;

public interface DriverStandingDataSource {
    void getDriverStandings(DriverStandingCallback callback);
}
