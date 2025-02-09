package com.the_coffe_coders.fastestlap.repository.driver;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;

public interface IDriverStandingsRepository {
    MutableLiveData<Result> fetchDriversStandings(long lastUpdate);
}
