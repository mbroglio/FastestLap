package com.the_coffe_coders.fastestlap.data.repository.weeklyrace;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;

public interface IRaceRepository {
    MutableLiveData<Result> fetchWeeklyRaces(long lastUpdate);

    MutableLiveData<Result> fetchNextRace(long lastUpdate);

    MutableLiveData<Result> fetchLastRace(long lastUpdate);
}

