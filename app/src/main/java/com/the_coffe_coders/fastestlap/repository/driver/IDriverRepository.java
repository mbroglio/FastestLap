package com.the_coffe_coders.fastestlap.repository.driver;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;

public interface IDriverRepository {
    MutableLiveData<Result> fetchDrivers(long lastUpdate);

    MutableLiveData<Result> fetchDriver(String driverId, long lastUpdate);

    MutableLiveData<Result> fetchDriversStandings(long lastUpdate);
}
