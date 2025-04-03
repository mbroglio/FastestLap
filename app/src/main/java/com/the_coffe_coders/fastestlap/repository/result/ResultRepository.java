package com.the_coffe_coders.fastestlap.repository.result;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.source.result.BaseRaceResultRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.result.RaceResultRemoteDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ResultRepository {
    private final Map<String, MutableLiveData<Result>> resultsCache;
    private final Map<String, Long> resultsTime;

    BaseRaceResultRemoteDataSource raceResultRemoteDataSource;

    public static ResultRepository instance;

    private ResultRepository() {
        resultsCache = new HashMap<>();
        resultsTime = new HashMap<>();
        raceResultRemoteDataSource = new RaceResultRemoteDataSource();
    }

    public static ResultRepository getInstance() {
        if (instance == null) {
            instance = new ResultRepository();
        }
        return instance;
    }

    public synchronized MutableLiveData<Result> fetchResults(String round) {
        if (resultsTime.get(round) == null || System.currentTimeMillis() - Objects.requireNonNull(resultsTime.get(round)) > 60000) {
            resultsCache.put(round, new MutableLiveData<>());
            loadResults(round);
        }

        return resultsCache.get(round);
    }

    public void loadResults(String round) {
        resultsCache.get(round).postValue(new Result.Loading("Fetching results from remote"));
        raceResultRemoteDataSource.getRaceResults(Integer.parseInt(round), new RaceResultCallback() {
            @Override
            public void onSuccess(Race race) {
                if(race != null) {
                    Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.LastRaceResultsSuccess(race));
                    resultsTime.put(round, System.currentTimeMillis());
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.Error(exception.getMessage()));
            }
        });
    }
}
