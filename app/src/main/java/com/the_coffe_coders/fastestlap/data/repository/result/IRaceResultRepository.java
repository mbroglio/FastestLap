package com.the_coffe_coders.fastestlap.data.repository.result;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;

public interface IRaceResultRepository {
    MutableLiveData<Result> fetchRaceResult(int round, long lastUpdate);

    MutableLiveData<Result> fetchAllRaceResults(long lastUpdate);

    MutableLiveData<Result> fetchLastRaceResults(long lastUpdate);
}
