package com.the_coffe_coders.fastestlap.data.repository.standings;

import com.the_coffe_coders.fastestlap.data.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;

import java.util.List;

public interface DriverStandingsResponseCallback {
    void onSuccessFromRemote(DriverStandingsAPIResponse driverAPIResponse, long lastUpdate);

    void onFailureFromRemote(Exception exception);

    void onSuccessFromLocal(List<Driver> driverList);

    void onSuccessFromLocal(DriverStandings driverStandings);

    //void onSuccessFromLocal(Driver driver);
    void onFailureFromLocal(Exception exception);
}
