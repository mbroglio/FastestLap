package com.the_coffe_coders.fastestlap.repository.driver;

import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;

import java.util.List;

public interface DriverStandingsResponseCallback {
    void onSuccessFromRemote(DriverStandingsAPIResponse driverStandingsAPIResponse, long lastUpdate);
    void onFailureFromRemote(Exception exception);
    void onSuccessFromLocal(DriverStandings driverStandings);
    void onFailureFromLocal(Exception exception);
}