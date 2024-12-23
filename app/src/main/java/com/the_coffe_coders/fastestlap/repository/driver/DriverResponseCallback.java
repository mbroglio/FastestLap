package com.the_coffe_coders.fastestlap.repository.driver;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.List;

public interface DriverResponseCallback {
    void onSuccessFromRemote(DriverStandingsAPIResponse driverAPIResponse, long lastUpdate);
    void onFailureFromRemote(Exception exception);
    void onSuccessFromLocal(List<Driver> driverList);
    //void onSuccessFromLocal(Driver driver);
    void onFailureFromLocal(Exception exception);
}
