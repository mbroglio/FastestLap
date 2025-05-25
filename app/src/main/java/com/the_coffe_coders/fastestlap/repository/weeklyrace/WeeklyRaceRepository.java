package com.the_coffe_coders.fastestlap.repository.weeklyrace;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.source.weeklyrace.LocalWeeklyRaceDataSource;
import com.the_coffe_coders.fastestlap.source.weeklyrace.JolpicaWeeklyRaceDataSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WeeklyRaceRepository {
    private static final String TAG = "WeeklyRaceRepository";
    private static WeeklyRaceRepository instance;
    private static final long FRESH_TIMEOUT = 60000;

    // Cache
    private final Map<String, MutableLiveData<Result>> raceCache;
    private final Map<String, Long> lastUpdateTimestamps;

    // Data sources
    private final JolpicaWeeklyRaceDataSource weeklyRaceRemoteDataSource;
    private final LocalWeeklyRaceDataSource localWeeklyRaceDataSource;

    private WeeklyRaceRepository(AppRoomDatabase appRoomDatabase) {
        this.raceCache = new HashMap<>();
        this.lastUpdateTimestamps = new HashMap<>();
        this.weeklyRaceRemoteDataSource = new JolpicaWeeklyRaceDataSource();
        this.localWeeklyRaceDataSource = LocalWeeklyRaceDataSource.getInstance(appRoomDatabase);

        // Initialize cache entries
        raceCache.put("next", new MutableLiveData<>());
        raceCache.put("last", new MutableLiveData<>());
        raceCache.put("all", new MutableLiveData<>());
    }

    public static WeeklyRaceRepository getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new WeeklyRaceRepository(appRoomDatabase);
        }
        return instance;
    }

    public synchronized MutableLiveData<Result> fetchNextWeeklyRace() {
        Log.d(TAG, "Fetching next weekly race");
        if (!lastUpdateTimestamps.containsKey("next") ||
                System.currentTimeMillis() - lastUpdateTimestamps.get("next") > FRESH_TIMEOUT) {
            loadNextRace();
        } else {
            Log.d(TAG, "Next race found in cache");
        }
        return raceCache.get("next");
    }

    private void loadNextRace() {
        raceCache.get("next").postValue(new Result.Loading("Loading next race"));
        try {
            weeklyRaceRemoteDataSource.getNextRace(new SingleWeeklyRaceCallback() {
                @Override
                public void onSuccess(WeeklyRace weeklyRace) {
                    Log.d(TAG, "Next race loaded from remote: " + weeklyRace);
                    if (weeklyRace != null) {
                        localWeeklyRaceDataSource.saveNextRace(weeklyRace);
                        lastUpdateTimestamps.put("next", System.currentTimeMillis());
                        Objects.requireNonNull(raceCache.get("next")).postValue(new Result.NextRaceSuccess(weeklyRace));
                    } else {
                        loadNextRaceFromLocal();
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.e(TAG, "Error loading next race: " + exception.getMessage());
                    loadNextRaceFromLocal();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching next race: " + e.getMessage());
            loadNextRaceFromLocal();
        }
    }

    private void loadNextRaceFromLocal() {
        localWeeklyRaceDataSource.getNextRace(new SingleWeeklyRaceCallback() {
            @Override
            public void onSuccess(WeeklyRace weeklyRace) {
                if (weeklyRace != null) {
                    lastUpdateTimestamps.put("next", System.currentTimeMillis());
                    Objects.requireNonNull(raceCache.get("next")).postValue(new Result.NextRaceSuccess(weeklyRace));
                    Log.d(TAG, "Next race loaded from local cache");
                } else {
                    Objects.requireNonNull(raceCache.get("next")).postValue(new Result.Error("Race not found in cache"));
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Objects.requireNonNull(raceCache.get("next")).postValue(new Result.Error(exception.getMessage()));
            }
        });
    }

    public synchronized MutableLiveData<Result> fetchLastWeeklyRace() {
        Log.d(TAG, "Fetching last weekly race");
        if (!lastUpdateTimestamps.containsKey("last") ||
                System.currentTimeMillis() - lastUpdateTimestamps.get("last") > FRESH_TIMEOUT) {
            loadLastRace();
        } else {
            Log.d(TAG, "Last race found in cache");
        }
        return raceCache.get("last");
    }

    private void loadLastRace() {
        raceCache.get("last").postValue(new Result.Loading("Loading last race"));
        weeklyRaceRemoteDataSource.getLastRace(new SingleWeeklyRaceCallback() {
            @Override
            public void onSuccess(WeeklyRace weeklyRace) {
                if (weeklyRace != null) {
                    localWeeklyRaceDataSource.saveLastRace(weeklyRace);
                    lastUpdateTimestamps.put("last", System.currentTimeMillis());
                    Objects.requireNonNull(raceCache.get("last")).postValue(new Result.NextRaceSuccess(weeklyRace));
                } else {
                    loadLastRaceFromLocal();
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error loading last race: " + exception.getMessage());
                loadLastRaceFromLocal();
            }
        });
    }

    private void loadLastRaceFromLocal() {
        localWeeklyRaceDataSource.getLastRace(new SingleWeeklyRaceCallback() {
            @Override
            public void onSuccess(WeeklyRace weeklyRace) {
                if (weeklyRace != null) {
                    lastUpdateTimestamps.put("last", System.currentTimeMillis());
                    Objects.requireNonNull(raceCache.get("last")).postValue(new Result.NextRaceSuccess(weeklyRace));
                    Log.d(TAG, "Last race loaded from local cache");
                } else {
                    Objects.requireNonNull(raceCache.get("last")).postValue(new Result.Error("Race not found in cache"));
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Objects.requireNonNull(raceCache.get("last")).postValue(new Result.Error(exception.getMessage()));
            }
        });
    }

    public synchronized MutableLiveData<Result> fetchWeeklyRaces() {
        Log.d(TAG, "Fetching all weekly races");
        if (!lastUpdateTimestamps.containsKey("all") ||
                System.currentTimeMillis() - lastUpdateTimestamps.get("all") > FRESH_TIMEOUT) {
            loadWeeklyRaces();
        } else {
            Log.d(TAG, "Weekly races found in cache");
        }
        return raceCache.get("all");
    }

    private void loadWeeklyRaces() {
        raceCache.get("all").postValue(new Result.Loading("Loading weekly races"));
        weeklyRaceRemoteDataSource.getWeeklyRaces(new WeeklyRacesCallback() {
            @Override
            public void onSuccess(List<WeeklyRace> weeklyRaces) {
                if (weeklyRaces != null && !weeklyRaces.isEmpty()) {
                    localWeeklyRaceDataSource.saveWeeklyRaces(weeklyRaces);
                    lastUpdateTimestamps.put("all", System.currentTimeMillis());
                    Objects.requireNonNull(raceCache.get("all")).postValue(new Result.WeeklyRaceSuccess(weeklyRaces));
                } else {
                    loadWeeklyRacesFromLocal();
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error loading weekly races: " + exception.getMessage());
                loadWeeklyRacesFromLocal();
            }
        });
    }

    private void loadWeeklyRacesFromLocal() {
        localWeeklyRaceDataSource.getWeeklyRaces(new WeeklyRacesCallback() {
            @Override
            public void onSuccess(List<WeeklyRace> weeklyRaces) {
                if (weeklyRaces != null && !weeklyRaces.isEmpty()) {
                    lastUpdateTimestamps.put("all", System.currentTimeMillis());
                    Objects.requireNonNull(raceCache.get("all")).postValue(new Result.WeeklyRaceSuccess(weeklyRaces));
                    Log.d(TAG, "Weekly races loaded from local cache");
                } else {
                    Objects.requireNonNull(raceCache.get("all")).postValue(new Result.Error("Weekly races not found in cache"));
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Objects.requireNonNull(raceCache.get("all")).postValue(new Result.Error(exception.getMessage()));
            }
        });
    }
}