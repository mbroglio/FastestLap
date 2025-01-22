package com.the_coffe_coders.fastestlap.repository.result;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;

public interface IRaceResultRepository {
    MutableLiveData<Result> fetchRaceResult(int round, long lastUpdate);

    MutableLiveData<Result> fetchRaceResult(String resultId, long lastUpdate);
}
