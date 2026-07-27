package com.the_coffe_coders.fastestlap.repository.f1.result;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.source.f1.result.JolpicaRaceResultDataSource;
import com.the_coffe_coders.fastestlap.source.f1.result.LocalRaceResultDataSource;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ResultRepository {
    private static final String TAG = "ResultRepository";
    private static ResultRepository instance;
    final JolpicaRaceResultDataSource jolpicaRaceResultDataSource;
    final LocalRaceResultDataSource localRaceResultDataSource;
    // Cache
    private final Map<String, MutableLiveData<Result>> resultsCache;
    private final Map<String, MutableLiveData<Result>> qualifyingResultsCache;
    private final Map<String, MutableLiveData<Result>> sprintResultsCache;
    private final Map<String, Long> lastUpdateTimestamps;
    private final Map<String, Long> qualifyingLastUpdateTimestamps;
    private final Map<String, Long> sprintLastUpdateTimestamps;
    private final NetworkUtils networkUtils;


    private ResultRepository(AppRoomDatabase appRoomDatabase, Context context) {
        resultsCache = new HashMap<>();
        qualifyingResultsCache = new HashMap<>();
        sprintResultsCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        qualifyingLastUpdateTimestamps = new HashMap<>();
        sprintLastUpdateTimestamps = new HashMap<>();
        jolpicaRaceResultDataSource = new JolpicaRaceResultDataSource();
        localRaceResultDataSource = LocalRaceResultDataSource.getInstance(appRoomDatabase);

        networkUtils = new NetworkUtils(context);
    }

    public static synchronized ResultRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new ResultRepository(appRoomDatabase, context);
        }
        return instance;
    }

    public synchronized MutableLiveData<Result> fetchResults(String round) {
        Log.d(TAG, "Fetching results for round: " + round);
        if (!resultsCache.containsKey(round)) {
            resultsCache.put(round, new MutableLiveData<>());
            loadResultsCacheFirst(round);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(round);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) {
                if (networkUtils.isConnected()) {
                    loadResults(round);
                }
            } else {
                Log.d(TAG, "Results found in cache for round: " + round);
            }
        }
        return resultsCache.get(round);
    }

    private void loadResultsCacheFirst(String round) {
        localRaceResultDataSource.getRaceResults(round, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race) {
                if (race != null) {
                    lastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Results loaded from local cache for round: " + round);
                    Long ts = lastUpdateTimestamps.get(round);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300000L;
                    if (networkUtils.isConnected() && isStale) {
                        loadResults(round);
                    }
                } else {
                    Log.d(TAG, "Results cache miss in local database for round: " + round);
                    loadResults(round);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error checking local database for results: " + exception.getMessage());
                loadResults(round);
            }
        });
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
                if (resultsCache.containsKey(round) && resultsCache.get(round) != null) {
                    resultsCache.get(round).postValue(new Result.Error(exception.getMessage()));
                }
            }
        });
    }

    public void loadResults(String round) {
        Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.Loading("Fetching results from remote"));

        if (networkUtils.isConnected()) {
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
        } else {
            Log.e(TAG, "Failed to load results: No internet connection");
            loadResultsFromLocal(round);
        }
    }

    public synchronized MutableLiveData<Result> fetchQualifyingResults(String round) {
        Log.d(TAG, "Fetching quali results for round: " + round);
        if (!qualifyingResultsCache.containsKey(round)) {
            qualifyingResultsCache.put(round, new MutableLiveData<>());
            loadQualifyingResultsCacheFirst(round);
        } else {
            Long lastUpdate = qualifyingLastUpdateTimestamps.get(round);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) {
                if (networkUtils.isConnected()) {
                    loadQualifyingResults(round);
                }
            } else {
                Log.d(TAG, "Qualifying results found in cache for round: " + round);
            }
        }
        return qualifyingResultsCache.get(round);
    }

    private void loadQualifyingResultsCacheFirst(String round) {
        localRaceResultDataSource.getQualifyingResults(round, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race) {
                if (race != null) {
                    qualifyingLastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Qualifying results loaded from local cache for round: " + round);
                    Long ts = qualifyingLastUpdateTimestamps.get(round);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300000L;
                    if (networkUtils.isConnected() && isStale) {
                        loadQualifyingResults(round);
                    }
                } else {
                    Log.d(TAG, "Qualifying results cache miss in local database for round: " + round);
                    loadQualifyingResults(round);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error checking local database for qualifying results: " + exception.getMessage());
                loadQualifyingResults(round);
            }
        });
    }

    private void loadQualifyingResults(String round) {
        Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.Loading("Fetching results from remote"));

        if (networkUtils.isConnected()) {
            try {
                jolpicaRaceResultDataSource.getQualifyingResults(Integer.parseInt(round), new RaceResultCallback() {
                    @Override
                    public void onSuccess(Race race) {
                        Log.d(TAG, "Results loaded: " + race);
                        if (race != null) {
                            localRaceResultDataSource.insertQualifyingResults(race);
                            qualifyingLastUpdateTimestamps.put(round, System.currentTimeMillis());
                            Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));

                        } else {
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
        } else {
            Log.e(TAG, "Failed to load results: No internet connection");
            loadQualifyingResultsFromLocal(round);
        }
    }

    private void loadQualifyingResultsFromLocal(String round) {
        localRaceResultDataSource.getQualifyingResults(round, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race) {
                if (race != null) {
                    qualifyingResultsCache.put(round, new MutableLiveData<>(new Result.RaceResultsSuccess(race)));
                    qualifyingLastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Qualifying results loaded from local cache for round: " + round);
                } else {
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

    public synchronized MutableLiveData<Result> fetchSprintResults(String round) {
        Log.d(TAG, "Fetching sprint results for round: " + round);
        if (!sprintResultsCache.containsKey(round)) {
            sprintResultsCache.put(round, new MutableLiveData<>());
            loadSprintResultsCacheFirst(round);
        } else {
            Long lastUpdate = sprintLastUpdateTimestamps.get(round);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) {
                if (networkUtils.isConnected()) {
                    loadSprintResults(round);
                }
            } else {
                Log.d(TAG, "Sprint results found in cache for round: " + round);
            }
        }
        return sprintResultsCache.get(round);
    }

    private void loadSprintResultsCacheFirst(String round) {
        localRaceResultDataSource.getSprintResults(round, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race) {
                if (race != null) {
                    sprintLastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Sprint results loaded from local cache for round: " + round);
                    Long ts = sprintLastUpdateTimestamps.get(round);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300000L;
                    if (networkUtils.isConnected() && isStale) {
                        loadSprintResults(round);
                    }
                } else {
                    Log.d(TAG, "Sprint results cache miss in local database for round: " + round);
                    loadSprintResults(round);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error checking local database for sprint results: " + exception.getMessage());
                loadSprintResults(round);
            }
        });
    }

    private void loadSprintResults(String round) {
        Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.Loading("Fetching results from remote"));

        if (networkUtils.isConnected()) {
            try {
                jolpicaRaceResultDataSource.getSprintResults(Integer.parseInt(round), new RaceResultCallback() {
                    @Override
                    public void onSuccess(Race race) {
                        Log.d(TAG, "Results loaded: " + race);
                        if (race != null) {
                            localRaceResultDataSource.insertSprintResults(race);
                            sprintLastUpdateTimestamps.put(round, System.currentTimeMillis());
                            Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));

                        } else {
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
        } else {
            Log.e(TAG, "Failed to load results: No internet connection");
            loadSprintResultsFromLocal(round);
        }
    }

    private void loadSprintResultsFromLocal(String round) {
        localRaceResultDataSource.getSprintResults(round, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race) {
                if (race != null) {
                    sprintResultsCache.put(round, new MutableLiveData<>(new Result.RaceResultsSuccess(race)));
                    sprintLastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Sprint results loaded from local cache for round: " + race);
                } else {
                    Log.e(TAG, "Sprint results not found in local cache for round: " + round);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error loading sprint results from local cache: " + exception.getMessage());
                if (sprintResultsCache.containsKey(round) && sprintResultsCache.get(round) != null) {
                    sprintResultsCache.get(round).postValue(new Result.Error(exception.getMessage()));
                }
            }
        });
    }
}