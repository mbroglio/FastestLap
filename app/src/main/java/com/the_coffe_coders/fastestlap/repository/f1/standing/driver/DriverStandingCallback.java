package com.the_coffe_coders.fastestlap.repository.f1.standing.driver;

import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.DriverStandings;

public interface DriverStandingCallback {
    void onDriverLoaded(DriverStandings driverStanding);

    void onError(Exception exception);
}
