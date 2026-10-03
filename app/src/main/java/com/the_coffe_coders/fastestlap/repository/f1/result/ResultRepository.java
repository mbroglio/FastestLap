package com.the_coffe_coders.fastestlap.repository.f1.result;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.source.f1.result.JolpicaRaceResultDataSource;
import com.the_coffe_coders.fastestlap.source.f1.result.LocalRaceResultDataSource;
import com.the_coffe_coders.fastestlap.source.f1.result.stint.OpenF1StintDataSource;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResultRepository {
    private static final String TAG = "ResultRepository";
    private static ResultRepository instance;
    final JolpicaRaceResultDataSource jolpicaRaceResultDataSource;
    final LocalRaceResultDataSource localRaceResultDataSource;
    final OpenF1StintDataSource openF1StintDataSource;
    // Cache
    private final Map<String, MutableLiveData<Result>> resultsCache;
    private final Map<String, MutableLiveData<Result>> qualifyingResultsCache;
    private final Map<String, MutableLiveData<Result>> sprintResultsCache;
    private final Map<String, MutableLiveData<Result>> stintsCache;
    private final Map<String, Long> lastUpdateTimestamps;
    private final Map<String, Long> qualifyingLastUpdateTimestamps;
    private final Map<String, Long> sprintLastUpdateTimestamps;
    private final NetworkUtils networkUtils;


    private ResultRepository(AppRoomDatabase appRoomDatabase, Context context) {
        resultsCache = new HashMap<>();
        qualifyingResultsCache = new HashMap<>();
        sprintResultsCache = new HashMap<>();
        stintsCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        qualifyingLastUpdateTimestamps = new HashMap<>();
        sprintLastUpdateTimestamps = new HashMap<>();
        jolpicaRaceResultDataSource = new JolpicaRaceResultDataSource();
        localRaceResultDataSource = LocalRaceResultDataSource.getInstance(appRoomDatabase);
        openF1StintDataSource = OpenF1StintDataSource.getInstance();

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
                    loadResults(round, true);
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
                if (race != null && race.getRaceResults() != null && !race.getRaceResults().isEmpty()) {
                    Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Results loaded from local cache for round: " + round);
                    Long ts = lastUpdateTimestamps.get(round);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300000L;
                    boolean missingStints = race.getRaceStints() == null || race.getRaceStints().isEmpty();
                    if (networkUtils.isConnected() && (isStale || missingStints)) {
                        loadResults(round, true);
                    }
                } else {
                    Log.d(TAG, "Results cache miss in local database for round: " + round);
                    loadResults(round, false);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error checking local database for results: " + exception.getMessage());
                loadResults(round, false);
            }
        });
    }

    public void loadResultsFromLocal(String round) {
        localRaceResultDataSource.getRaceResults(round, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race) {
                if (race != null && race.getRaceResults() != null && !race.getRaceResults().isEmpty()) {
                    resultsCache.put(round, new MutableLiveData<>(new Result.RaceResultsSuccess(race)));
                    lastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Results loaded from local cache for round: " + race);
                } else {
                    Log.e(TAG, "Results not found in local cache for round: " + round);
                    if (resultsCache.containsKey(round) && resultsCache.get(round) != null) {
                        resultsCache.get(round).postValue(new Result.Error("No race results found in cache"));
                    }
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
        loadResults(round, false);
    }

    public void loadResults(String round, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.Loading("Fetching results from remote"));
        }

        if (networkUtils.isConnected()) {
            try {
                jolpicaRaceResultDataSource.getRaceResults(Integer.parseInt(round), new RaceResultCallback() {
                    @Override
                    public void onSuccess(Race race) {
                        Log.d(TAG, "Results loaded: " + race);
                        if (race != null) {
                            saveAndPostRace(round, race);

                            if (race.getRaceName() != null) {
                                openF1StintDataSource.getStints(race.getRaceName(), "Race", new StintCallback() {
                                    @Override
                                    public void onSuccess(List<Stint> stints) {
                                        if (stints != null && !stints.isEmpty()) {
                                            race.setRaceStints(stints);
                                            localRaceResultDataSource.insertRaceResults(race);
                                            if (resultsCache.containsKey(round) && resultsCache.get(round) != null) {
                                                resultsCache.get(round).postValue(new Result.RaceResultsSuccess(race));
                                            }
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception exception) {
                                        Log.w(TAG, "Non-blocking stint fetch failed for race round " + round + ": " + exception.getMessage());
                                    }
                                });
                            }
                        } else {
                            Log.e(TAG, "Results not found in cache for round: " + round);
                            loadResultsFromLocal(round);
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        Log.e(TAG, "Error loading results: " + exception.getMessage());
                        loadResultsFromLocal(round);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading results: " + e.getMessage());
                loadResultsFromLocal(round);
            }
        } else {
            Log.e(TAG, "Failed to load results: No internet connection");
            loadResultsFromLocal(round);
        }
    }

    private void saveAndPostRace(String round, Race race) {
        if (race == null) return;

        // 1. Preservazione in memoria immediata: se la cache in RAM aveva già gli stint per questo round,
        // li travasiamo subito nel nuovo oggetto Race prima del dispatch alla UI
        if (resultsCache.containsKey(round) && resultsCache.get(round) != null) {
            Result prev = resultsCache.get(round).getValue();
            if (prev instanceof Result.RaceResultsSuccess) {
                Race existingMem = ((Result.RaceResultsSuccess) prev).getData();
                if (existingMem != null) {
                    if ((race.getRaceStints() == null || race.getRaceStints().isEmpty())
                            && existingMem.getRaceStints() != null && !existingMem.getRaceStints().isEmpty()) {
                        race.setRaceStints(existingMem.getRaceStints());
                        Log.d(TAG, "Preserved in-memory raceStints for round: " + round);
                    }
                    if ((race.getSprintStints() == null || race.getSprintStints().isEmpty())
                            && existingMem.getSprintStints() != null && !existingMem.getSprintStints().isEmpty()) {
                        race.setSprintStints(existingMem.getSprintStints());
                    }
                }
            }
        }

        // 2. Salvataggio su database locale Room con preservazione su disco:
        // se la memoria era vuota ma il DB conteneva già gli stint, il callback aggiorna la UI
        localRaceResultDataSource.insertRaceResults(race, () -> {
            if (resultsCache.containsKey(round) && resultsCache.get(round) != null) {
                resultsCache.get(round).postValue(new Result.RaceResultsSuccess(race));
            }
        });

        lastUpdateTimestamps.put(round, System.currentTimeMillis());
        Objects.requireNonNull(resultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
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
                    loadQualifyingResults(round, true);
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
                if (race != null && race.getQualifyingResults() != null && !race.getQualifyingResults().isEmpty()) {
                    Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Qualifying results loaded from local cache for round: " + round);
                    Long ts = qualifyingLastUpdateTimestamps.get(round);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300000L;
                    if (networkUtils.isConnected() && isStale) {
                        loadQualifyingResults(round, true);
                    }
                } else {
                    Log.d(TAG, "Qualifying results cache miss in local database for round: " + round);
                    loadQualifyingResults(round, false);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error checking local database for qualifying results: " + exception.getMessage());
                loadQualifyingResults(round, false);
            }
        });
    }

    private void loadQualifyingResults(String round) {
        loadQualifyingResults(round, false);
    }

    private void loadQualifyingResults(String round, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.Loading("Fetching results from remote"));
        }

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
                        if (!isBackgroundRefresh) {
                            Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.Error(exception.getMessage()));
                        }
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
                if (race != null && race.getQualifyingResults() != null && !race.getQualifyingResults().isEmpty()) {
                    qualifyingResultsCache.put(round, new MutableLiveData<>(new Result.RaceResultsSuccess(race)));
                    qualifyingLastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(qualifyingResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Qualifying results loaded from local cache for round: " + round);
                } else {
                    Log.e(TAG, "Qualifying results not found in local cache for round: " + round);
                    if (qualifyingResultsCache.containsKey(round) && qualifyingResultsCache.get(round) != null) {
                        qualifyingResultsCache.get(round).postValue(new Result.Error("No qualifying results found in cache"));
                    }
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error loading qualifying results from local cache: " + exception.getMessage());
                if (qualifyingResultsCache.containsKey(round) && qualifyingResultsCache.get(round) != null) {
                    qualifyingResultsCache.get(round).postValue(new Result.Error(exception.getMessage()));
                }
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
                    loadSprintResults(round, true);
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
                if (race != null && race.getSprintResults() != null && !race.getSprintResults().isEmpty()) {
                    Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Sprint results loaded from local cache for round: " + round);
                    Long ts = sprintLastUpdateTimestamps.get(round);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300000L;
                    boolean missingStints = race.getSprintStints() == null || race.getSprintStints().isEmpty();
                    if (networkUtils.isConnected() && (isStale || missingStints)) {
                        loadSprintResults(round, true);
                    }
                } else {
                    Log.d(TAG, "Sprint results cache miss in local database for round: " + round);
                    loadSprintResults(round, false);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error checking local database for sprint results: " + exception.getMessage());
                loadSprintResults(round, false);
            }
        });
    }

    private void loadSprintResults(String round) {
        loadSprintResults(round, false);
    }

    private void loadSprintResults(String round, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.Loading("Fetching results from remote"));
        }

        if (networkUtils.isConnected()) {
            try {
                jolpicaRaceResultDataSource.getSprintResults(Integer.parseInt(round), new RaceResultCallback() {
                    @Override
                    public void onSuccess(Race race) {
                        Log.d(TAG, "Sprint results loaded: " + race);
                        if (race != null) {
                            saveAndPostSprint(round, race);

                            if (race.getRaceName() != null) {
                                openF1StintDataSource.getStints(race.getRaceName(), "Sprint", new StintCallback() {
                                    @Override
                                    public void onSuccess(List<Stint> stints) {
                                        if (stints != null && !stints.isEmpty()) {
                                            race.setSprintStints(stints);
                                            localRaceResultDataSource.insertSprintResults(race);
                                            if (sprintResultsCache.containsKey(round) && sprintResultsCache.get(round) != null) {
                                                sprintResultsCache.get(round).postValue(new Result.RaceResultsSuccess(race));
                                            }
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception exception) {
                                        Log.w(TAG, "Non-blocking stint fetch failed for sprint round " + round + ": " + exception.getMessage());
                                    }
                                });
                            }
                        } else {
                            Log.e(TAG, "Sprint results not found in remote for round: " + round);
                            loadSprintResultsFromLocal(round);
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        Log.e(TAG, "Error loading sprint results: " + exception.getMessage());
                        loadSprintResultsFromLocal(round);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading sprint results: " + e.getMessage());
                loadSprintResultsFromLocal(round);
            }
        } else {
            Log.e(TAG, "Failed to load results: No internet connection");
            loadSprintResultsFromLocal(round);
        }
    }

    private void saveAndPostSprint(String round, Race race) {
        if (race == null) return;

        // 1. Preservazione in memoria immediata: se la cache in RAM aveva già gli sprintStints per questo round,
        // li travasiamo subito nel nuovo oggetto Race prima del dispatch alla UI
        if (sprintResultsCache.containsKey(round) && sprintResultsCache.get(round) != null) {
            Result prev = sprintResultsCache.get(round).getValue();
            if (prev instanceof Result.RaceResultsSuccess) {
                Race existingMem = ((Result.RaceResultsSuccess) prev).getData();
                if (existingMem != null) {
                    if ((race.getSprintStints() == null || race.getSprintStints().isEmpty())
                            && existingMem.getSprintStints() != null && !existingMem.getSprintStints().isEmpty()) {
                        race.setSprintStints(existingMem.getSprintStints());
                        Log.d(TAG, "Preserved in-memory sprintStints for round: " + round);
                    }
                    if ((race.getRaceStints() == null || race.getRaceStints().isEmpty())
                            && existingMem.getRaceStints() != null && !existingMem.getRaceStints().isEmpty()) {
                        race.setRaceStints(existingMem.getRaceStints());
                    }
                }
            }
        }

        // 2. Salvataggio su database locale Room con preservazione su disco:
        // se la memoria era vuota ma il DB conteneva già gli stint, il callback aggiorna la UI
        localRaceResultDataSource.insertSprintResults(race, () -> {
            if (sprintResultsCache.containsKey(round) && sprintResultsCache.get(round) != null) {
                sprintResultsCache.get(round).postValue(new Result.RaceResultsSuccess(race));
            }
        });

        sprintLastUpdateTimestamps.put(round, System.currentTimeMillis());
        Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
    }

    private void loadSprintResultsFromLocal(String round) {
        localRaceResultDataSource.getSprintResults(round, new RaceResultCallback() {
            @Override
            public void onSuccess(Race race) {
                if (race != null && race.getSprintResults() != null && !race.getSprintResults().isEmpty()) {
                    sprintResultsCache.put(round, new MutableLiveData<>(new Result.RaceResultsSuccess(race)));
                    sprintLastUpdateTimestamps.put(round, System.currentTimeMillis());
                    Objects.requireNonNull(sprintResultsCache.get(round)).postValue(new Result.RaceResultsSuccess(race));
                    Log.d(TAG, "Sprint results loaded from local cache for round: " + race);
                } else {
                    Log.e(TAG, "Sprint results not found in local cache for round: " + round);
                    if (sprintResultsCache.containsKey(round) && sprintResultsCache.get(round) != null) {
                        sprintResultsCache.get(round).postValue(new Result.Error("No sprint results found in cache"));
                    }
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

    public synchronized MutableLiveData<Result> fetchStints(String eventName, String sessionName) {
        String key = (eventName != null ? eventName : "latest") + "_" + (sessionName != null ? sessionName : "Race");
        Log.d(TAG, "Fetching stints for key: " + key);

        if (!stintsCache.containsKey(key)) {
            stintsCache.put(key, new MutableLiveData<>());
        }

        MutableLiveData<Result> liveData = stintsCache.get(key);
        Result current = Objects.requireNonNull(liveData).getValue();
        if (current instanceof Result.StintsSuccess) {
            List<Stint> cached = ((Result.StintsSuccess) current).getData();
            if (cached != null && !cached.isEmpty()) {
                return liveData;
            }
        }

        // Controllo preventivo: se gli stint sono già disponibili nella cache locale per questo evento,
        // li restituiamo subito senza attendere la rete
        List<Stint> localPreCheck = findStintsInLocalCache(eventName, sessionName);
        if (localPreCheck != null && !localPreCheck.isEmpty()) {
            liveData.postValue(new Result.StintsSuccess(localPreCheck));
            if (!networkUtils.isConnected()) {
                return liveData;
            }
        } else {
            liveData.postValue(new Result.Loading("Fetching stints from OpenF1"));
        }

        if (networkUtils.isConnected()) {
            openF1StintDataSource.getStints(eventName, sessionName, new StintCallback() {
                @Override
                public void onSuccess(java.util.List<Stint> stints) {
                    if (stints != null && !stints.isEmpty()) {
                        Log.d(TAG, "Stints loaded successfully: " + stints.size());
                        liveData.postValue(new Result.StintsSuccess(stints));
                        enrichCachedRacesWithStints(eventName, sessionName, stints);
                    } else {
                        Log.d(TAG, "Stints returned empty list from OpenF1, checking local fallback");
                        List<Stint> fallback = findStintsInLocalCache(eventName, sessionName);
                        if (fallback != null && !fallback.isEmpty()) {
                            liveData.postValue(new Result.StintsSuccess(fallback));
                        } else {
                            liveData.postValue(new Result.Error("No stints available"));
                        }
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.e(TAG, "Error fetching stints from OpenF1: " + exception.getMessage() + ", checking local fallback");
                    List<Stint> fallback = findStintsInLocalCache(eventName, sessionName);
                    if (fallback != null && !fallback.isEmpty()) {
                        liveData.postValue(new Result.StintsSuccess(fallback));
                    } else {
                        liveData.postValue(new Result.Error(exception.getMessage()));
                    }
                }
            });
        } else {
            Log.e(TAG, "Failed to load stints: No internet connection, checking local fallback");
            List<Stint> fallback = findStintsInLocalCache(eventName, sessionName);
            if (fallback != null && !fallback.isEmpty()) {
                liveData.postValue(new Result.StintsSuccess(fallback));
            } else {
                liveData.postValue(new Result.Error("No network connection"));
            }
        }

        return liveData;
    }

    private List<Stint> findStintsInLocalCache(String eventName, String sessionName) {
        if (eventName == null) return null;
        boolean isSprint = "Sprint".equalsIgnoreCase(sessionName);
        for (Map.Entry<String, MutableLiveData<Result>> entry : (isSprint ? sprintResultsCache : resultsCache).entrySet()) {
            Result r = entry.getValue().getValue();
            if (r instanceof Result.RaceResultsSuccess) {
                Race race = ((Result.RaceResultsSuccess) r).getData();
                if (race != null && eventName.equalsIgnoreCase(race.getRaceName())) {
                    List<Stint> list = isSprint ? race.getSprintStints() : race.getRaceStints();
                    if (list != null && !list.isEmpty()) {
                        return list;
                    }
                    if (race.getStints() != null && !race.getStints().isEmpty()) {
                        return race.getStints();
                    }
                }
            }
        }
        return null;
    }

    private void enrichCachedRacesWithStints(String eventName, String sessionName, List<Stint> stints) {
        if (stints == null || stints.isEmpty() || eventName == null) return;
        boolean isSprint = "Sprint".equalsIgnoreCase(sessionName);
        for (Map.Entry<String, MutableLiveData<Result>> entry : (isSprint ? sprintResultsCache : resultsCache).entrySet()) {
            Result r = entry.getValue().getValue();
            if (r instanceof Result.RaceResultsSuccess) {
                Race race = ((Result.RaceResultsSuccess) r).getData();
                if (race != null && eventName.equalsIgnoreCase(race.getRaceName())) {
                    if (isSprint) {
                        race.setSprintStints(stints);
                        localRaceResultDataSource.insertSprintResults(race);
                    } else {
                        race.setRaceStints(stints);
                        localRaceResultDataSource.insertRaceResults(race);
                    }
                    entry.getValue().postValue(new Result.RaceResultsSuccess(race));
                }
            }
        }
    }
}