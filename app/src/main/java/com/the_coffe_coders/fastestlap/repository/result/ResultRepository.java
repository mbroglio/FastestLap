package com.the_coffe_coders.fastestlap.repository.result;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.source.result.BaseRaceResultRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.result.RaceResultRemoteDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultRepository {
    private final Map<String, MutableLiveData<Result>> resultsCache;
    private final Map<String, Long> resultsTime;

    private final String TAG = "ResultRepository";

    private final MutableLiveData<Result> allResults;
    private long lastTimeAllResults;
    private List<Race> raceList;

    BaseRaceResultRemoteDataSource raceResultRemoteDataSource;

    public static ResultRepository instance;

    private ResultRepository() {
        resultsCache = new HashMap<>();
        resultsTime = new HashMap<>();
        allResults = new MutableLiveData<>();
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
        Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.Loading("Fetching results from remote"));
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

    public void getAllRaceResults(int numberOfRaces, RaceResultCallback callback) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 1; i <= numberOfRaces; i++) {
            raceResultRemoteDataSource.fetchRaceResult(i, 0, successCount, failureCount, numberOfRaces, callback);
        }
    }

    public MutableLiveData<Result> fetchAllRaceResults(int numberOfRaces) {
        raceList = new ArrayList<>();
        getAllRaceResults(numberOfRaces, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race) {
                addRaces(race, numberOfRaces);
            }

            @Override
            public void onFailure(Exception exception) {
                allResults.postValue(new Result.Error(exception.getMessage()));
            }
        });
        return allResults;
    }

    private synchronized void addRaces(Race race, int numberOfRaces) {
        raceList.add(race);
        Log.i(TAG, race.toString());
        if (raceList.size() == numberOfRaces) {
            allResults.setValue(new Result.RaceSuccess(raceList));
            Log.i(TAG, "posting value!!! + " + raceList.size());
        }
    }
}
