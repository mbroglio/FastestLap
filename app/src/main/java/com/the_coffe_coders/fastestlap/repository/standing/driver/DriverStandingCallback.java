package com.the_coffe_coders.fastestlap.repository.standing.driver;

import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;

public interface DriverStandingCallback {
    void onSuccess(DriverStandings driverStanding);

    void onFailure(Exception exception);
}
