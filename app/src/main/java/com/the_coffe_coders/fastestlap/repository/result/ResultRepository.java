package com.the_coffe_coders.fastestlap.repository.result;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.source.result.JolpicaRaceResultDataSource;
import com.the_coffe_coders.fastestlap.source.result.LocalRaceResultDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultRepository {
    private static final String TAG = "ResultRepository";
    private static ResultRepository instance;

    // Cache
    private final Map<String, MutableLiveData<Result>> resultsCache;
    private final Map<String, Long> lastUpdateTimestamps;

    // Data Sources
    private final MutableLiveData<Result> allResults;
    JolpicaRaceResultDataSource jolpicaRaceResultDataSource;
    LocalRaceResultDataSource localRaceResultDataSource;
    AppRoomDatabase appRoomDatabase;
    private List<Race> raceList;

    private ResultRepository(AppRoomDatabase appRoomDatabase) {
        resultsCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        allResults = new MutableLiveData<>();
        jolpicaRaceResultDataSource = new JolpicaRaceResultDataSource();
        localRaceResultDataSource = LocalRaceResultDataSource.getInstance(appRoomDatabase);
    }

    public static synchronized ResultRepository getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new ResultRepository(appRoomDatabase);
        }
        return instance;
    }

    public synchronized MutableLiveData<Result> fetchResults(String round) {
        Log.d(TAG, "Fetching results for round: " + round);
        if (!resultsCache.containsKey(round) || !lastUpdateTimestamps.containsKey(round) || lastUpdateTimestamps.get(round) == null) {
            resultsCache.put(round, new MutableLiveData<>());
            loadResults(round);
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(round) > 60000) {
            loadResults(round);
        } else {
            Log.d(TAG, "Results found in cache for round: " + round);
        }
        return resultsCache.get(round);
    }

    public void loadResultsFromLocal(String round) {
        localRaceResultDataSource.getRaceResults(round, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race) {
                if (race != null) {
                    resultsCache.put(round, new MutableLiveData<>(new Result.LastRaceResultsSuccess(race)));
                    lastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.LastRaceResultsSuccess(race));
                    Log.d(TAG, "Results loaded from local cache for round: " + race);
                } else {
                    Log.e(TAG, "Results not found in local cache for round: " + round);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error loading results from local cache: " + exception.getMessage());
                loadResultsFromLocal(round);
            }
        });
    }

    public void loadResults(String round) {
        resultsCache.get(round).postValue(new Result.Loading("Fetching results from remote"));
        try {
            jolpicaRaceResultDataSource.getRaceResults(Integer.parseInt(round), new RaceResultCallback() {
                @Override
                public void onSuccess(Race race) {
                    Log.d(TAG, "Results loaded: " + race);
                    if (race != null) {
                        localRaceResultDataSource.insertRaceResults(race);
                        lastUpdateTimestamps.put(round, System.currentTimeMillis());
                        Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.LastRaceResultsSuccess(race));
                    } else {
                        Log.e(TAG, "Results not found in cache for round: " + round);
                        loadResultsFromLocal(round);
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.e(TAG, "Error loading results: " + exception.getMessage());
                    Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.Error(exception.getMessage()));
                    loadResultsFromLocal(round);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading results: " + e.getMessage());
        }
    }

    public void getAllRaceResults(int numberOfRaces, RaceResultCallback callback) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 1; i <= numberOfRaces; i++) {
            jolpicaRaceResultDataSource.fetchRaceResult(i, 0, successCount, failureCount, numberOfRaces, callback, false);
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