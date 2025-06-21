package com.the_coffe_coders.fastestlap.repository.result;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.source.result.JolpicaRaceResultDataSource;
import com.the_coffe_coders.fastestlap.source.result.LocalRaceResultDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ResultRepository {
    private static final String TAG = "ResultRepository";
    private static ResultRepository instance;

    // Cache
    private final Map<String, MutableLiveData<Result>> resultsCache;
    private final Map<String, MutableLiveData<Result>> qualifyingResultsCache;
    private final Map<String, MutableLiveData<Result>> sprintResultsCache;
    private final Map<String, Long> lastUpdateTimestamps;
    private final Map<String, Long> qualifyingLastUpdateTimestamps;
    private final Map<String, Long> sprintLastUpdateTimestamps;
    final JolpicaRaceResultDataSource jolpicaRaceResultDataSource;
    final LocalRaceResultDataSource localRaceResultDataSource;

    private ResultRepository(AppRoomDatabase appRoomDatabase) {
        resultsCache = new HashMap<>();
        qualifyingResultsCache = new HashMap<>();
        sprintResultsCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        qualifyingLastUpdateTimestamps = new HashMap<>();
        sprintLastUpdateTimestamps = new HashMap<>();
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
                    resultsCache.put(round, new MutableLiveData<>(new Result.RaceResultsSuccess(race)));
                    lastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
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
        Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.Loading("Fetching results from remote"));
        try {
            jolpicaRaceResultDataSource.getRaceResults(Integer.parseInt(round), new RaceResultCallback() {
                @Override
                public void onSuccess(Race race) {
                    Log.d(TAG, "Results loaded: " + race);
                    if (race != null) {
                        localRaceResultDataSource.insertRaceResults(race);
                        lastUpdateTimestamps.put(round, System.currentTimeMillis());
                        Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
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

    public synchronized MutableLiveData<Result> fetchQualifyingResults(String round) {
        Log.d(TAG, "Fetching quali results for round: " + round);
        if (!qualifyingResultsCache.containsKey(round) || !qualifyingLastUpdateTimestamps.containsKey(round) || qualifyingLastUpdateTimestamps.get(round) == null) {
            qualifyingResultsCache.put(round, new MutableLiveData<>());
            loadQualifyingResults(round);
        } else if (System.currentTimeMillis() - qualifyingLastUpdateTimestamps.get(round) > 60000) {
            loadQualifyingResults(round);
        } else {
            Log.d(TAG, "Results found in cache for round: " + round);
        }
        return qualifyingResultsCache.get(round);
    }

    private void loadQualifyingResults(String round) {
        Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.Loading("Fetching results from remote"));
        try {
            jolpicaRaceResultDataSource.getQualifyingResults(Integer.parseInt(round), new RaceResultCallback() {
                @Override
                public void onSuccess(Race race) {
                    Log.d(TAG, "Results loaded: " + race);
                    if(race != null){
                        localRaceResultDataSource.insertQualifyingResults(race);
                        qualifyingLastUpdateTimestamps.put(round, System.currentTimeMillis());
                        Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));

                    }else{
                        Log.e(TAG, "Results not found in cache for round: " + round);
                        loadQualifyingResultsFromLocal(round);
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.e(TAG, "Error loading results: " + exception.getMessage());
                    Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.Error(exception.getMessage()));
                    loadQualifyingResultsFromLocal(round);

                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading results: " + e.getMessage());
        }
    }

    private void loadQualifyingResultsFromLocal(String round) {
        localRaceResultDataSource.getQualifyingResults(round, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race){
                if(race != null){
                    qualifyingResultsCache.put(round, new MutableLiveData<>(new Result.RaceResultsSuccess(race)));
                    qualifyingLastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Qualifying results loaded from local cache for round: " + round);
                }else{
                    Log.e(TAG, "Qualifying results not found in local cache for round: " + round);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error loading qualifying results from local cache: " + exception.getMessage());
                Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.Error(exception.getMessage()));
            }
        });
    }

    public MutableLiveData<Result> fetchSprintResults(String round) {
        Log.d(TAG, "Fetching results for round: " + round);
        if (!sprintResultsCache.containsKey(round) || !sprintLastUpdateTimestamps.containsKey(round) || sprintLastUpdateTimestamps.get(round) == null) {
            sprintResultsCache.put(round, new MutableLiveData<>());
            loadSprintResults(round);
        } else if (System.currentTimeMillis() - sprintLastUpdateTimestamps.get(round) > 60000) {
            loadSprintResults(round);
        } else {
            Log.d(TAG, "Results found in cache for round: " + round);
        }
        return sprintResultsCache.get(round);
    }

    private void loadSprintResults(String round) {
        Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.Loading("Fetching results from remote"));

        try {
            jolpicaRaceResultDataSource.getSprintResults(Integer.parseInt(round), new RaceResultCallback() {
                @Override
                public void onSuccess(Race race) {
                    Log.d(TAG, "Results loaded: " + race);
                    if(race != null){
                        localRaceResultDataSource.insertSprintResults(race);
                        sprintLastUpdateTimestamps.put(round, System.currentTimeMillis());
                        Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));

                    }else{
                        Log.e(TAG, "Results not found in cache for round: " + round);
                        loadSprintResultsFromLocal(round);
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.e(TAG, "Error loading results: " + exception.getMessage());
                    Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.Error(exception.getMessage()));
                    loadSprintResultsFromLocal(round);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading results: " + e.getMessage());
        }
    }

    private void loadSprintResultsFromLocal(String round) {
        localRaceResultDataSource.getSprintResults(round, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race){
                if(race != null){
                    sprintResultsCache.put(round, new MutableLiveData<>(new Result.RaceResultsSuccess(race)));
                    sprintLastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Sprint results loaded from local cache for round: " + race);
                }else{
                    Log.e(TAG, "Sprint results not found in local cache for round: " + round);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error loading sprint results from local cache: " + exception.getMessage());
                loadSprintResultsFromLocal(round);
            }
        });
    }
}