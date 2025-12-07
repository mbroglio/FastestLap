package com.the_coffe_coders.fastestlap.source.f1.standing.driver;

import com.the_coffe_coders.fastestlap.repository.f1.standing.driver.DriverStandingCallback;

public interface DriverStandingDataSource {
    void getDriverStandings(DriverStandingCallback callback);
}
