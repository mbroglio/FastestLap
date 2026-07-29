package com.the_coffe_coders.fastestlap.repository.f1.weeklyrace;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.source.f1.weeklyrace.JolpicaWeeklyRaceDataSource;
import com.the_coffe_coders.fastestlap.source.f1.weeklyrace.LocalWeeklyRaceDataSource;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WeeklyRaceRepository {
    private static final String TAG = "WeeklyRaceRepository";
    private static final long FRESH_TIMEOUT = 86400000L; // 24 hours
    private static WeeklyRaceRepository instance;
    private final Map<String, MutableLiveData<Result>> raceCache;
    private final Map<String, Long> lastUpdateTimestamps;
    private final JolpicaWeeklyRaceDataSource weeklyRaceRemoteDataSource;
    private final LocalWeeklyRaceDataSource localWeeklyRaceDataSource;
    private final NetworkUtils networkUtils;

    private WeeklyRaceRepository(AppRoomDatabase appRoomDatabase, Context context) {
        this.raceCache = new HashMap<>();
        this.lastUpdateTimestamps = new HashMap<>();
        this.weeklyRaceRemoteDataSource = new JolpicaWeeklyRaceDataSource();
        this.localWeeklyRaceDataSource = LocalWeeklyRaceDataSource.getInstance(appRoomDatabase);
        this.networkUtils = new NetworkUtils(context);

        raceCache.put("next", new MutableLiveData<>());
        raceCache.put("last", new MutableLiveData<>());
        raceCache.put("all", new MutableLiveData<>());
    }

    public static WeeklyRaceRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new WeeklyRaceRepository(appRoomDatabase, context);
        }
        return instance;
    }

    public synchronized LiveData<Result> fetchNextWeeklyRace() {
        Log.d(TAG, "Fetching next weekly race");
        if (!lastUpdateTimestamps.containsKey("next") ||
                System.currentTimeMillis() - lastUpdateTimestamps.get("next") > FRESH_TIMEOUT) {
            loadNextRace();
        } else {
            Log.d(TAG, "Next race found in cache");
        }
        return raceCache.get("next");
    }

    public synchronized LiveData<Result> fetchLastWeeklyRace() {
        Log.d(TAG, "Fetching last weekly race");
        if (!lastUpdateTimestamps.containsKey("last") ||
                System.currentTimeMillis() - lastUpdateTimestamps.get("last") > FRESH_TIMEOUT) {
            loadLastRace();
        } else {
            Log.d(TAG, "Last race found in cache");
        }
        return raceCache.get("last");
    }

    public synchronized LiveData<Result> fetchWeeklyRaces() {
        Log.d(TAG, "Fetching all weekly races");
        if (!lastUpdateTimestamps.containsKey("all") ||
                System.currentTimeMillis() - lastUpdateTimestamps.get("all") > FRESH_TIMEOUT) {
            loadWeeklyRaces();
        } else {
            Log.d(TAG, "Weekly races found in cache");
        }
        return raceCache.get("all");
    }

    private void loadNextRace() {
        raceCache.get("next").postValue(new Result.Loading("Loading next race"));

        localWeeklyRaceDataSource.getNextRace(new SingleWeeklyRaceCallback() {
            @Override
            public void onSuccess(WeeklyRace weeklyRace) {
                if (weeklyRace != null) {
                    lastUpdateTimestamps.put("next", System.currentTimeMillis());
                    Objects.requireNonNull(raceCache.get("next")).postValue(new Result.NextRaceSuccess(weeklyRace));
                    Log.d(TAG, "Next race loaded from local cache");
                }
                if (networkUtils.isConnected()) {
                    fetchNextRaceFromRemote(weeklyRace == null);
                } else if (weeklyRace == null) {
                    Objects.requireNonNull(raceCache.get("next")).postValue(new Result.Error("No next weekly race found in local database"));
                }
            }

            @Override
            public void onFailure(Exception exception) {
                if (networkUtils.isConnected()) {
                    fetchNextRaceFromRemote(true);
                } else {
                    Objects.requireNonNull(raceCache.get("next")).postValue(new Result.Error(exception.getMessage()));
                }
            }
        });
    }

    private void fetchNextRaceFromRemote(boolean isInitialLoad) {
        try {
            weeklyRaceRemoteDataSource.getNextRace(new SingleWeeklyRaceCallback() {
                @Override
                public void onSuccess(WeeklyRace weeklyRace) {
                    Log.d(TAG, "Next race loaded from remote: " + weeklyRace);
                    if (weeklyRace != null) {
                        localWeeklyRaceDataSource.saveSingleWeeklyRace(weeklyRace);
                        lastUpdateTimestamps.put("next", System.currentTimeMillis());
                        Objects.requireNonNull(raceCache.get("next")).postValue(new Result.NextRaceSuccess(weeklyRace));
                    } else if (isInitialLoad) {
                        Objects.requireNonNull(raceCache.get("next")).postValue(new Result.Error("No next race found from remote"));
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.e(TAG, "Error fetching next race from remote: " + exception.getMessage());
                    if (isInitialLoad) {
                        Objects.requireNonNull(raceCache.get("next")).postValue(new Result.Error(exception.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching next race from remote: " + e.getMessage());
            if (isInitialLoad) {
                Objects.requireNonNull(raceCache.get("next")).postValue(new Result.Error(e.getMessage()));
            }
        }
    }

    private void loadLastRace() {
        raceCache.get("last").postValue(new Result.Loading("Loading last race"));

        localWeeklyRaceDataSource.getLastRace(new SingleWeeklyRaceCallback() {
            @Override
            public void onSuccess(WeeklyRace weeklyRace) {
                if (weeklyRace != null) {
                    lastUpdateTimestamps.put("last", System.currentTimeMillis());
                    Objects.requireNonNull(raceCache.get("last")).postValue(new Result.NextRaceSuccess(weeklyRace));
                    Log.d(TAG, "Last race loaded from local cache");
                }
                if (networkUtils.isConnected()) {
                    fetchLastRaceFromRemote(weeklyRace == null);
                } else if (weeklyRace == null) {
                    Objects.requireNonNull(raceCache.get("last")).postValue(new Result.Error("No last weekly race found in local database"));
                }
            }

            @Override
            public void onFailure(Exception exception) {
                if (networkUtils.isConnected()) {
                    fetchLastRaceFromRemote(true);
                } else {
                    Objects.requireNonNull(raceCache.get("last")).postValue(new Result.Error(exception.getMessage()));
                }
            }
        });
    }

    private void fetchLastRaceFromRemote(boolean isInitialLoad) {
        try {
            weeklyRaceRemoteDataSource.getLastRace(new SingleWeeklyRaceCallback() {
                @Override
                public void onSuccess(WeeklyRace weeklyRace) {
                    Log.d(TAG, "Last race loaded from remote: " + weeklyRace);
                    if (weeklyRace != null) {
                        localWeeklyRaceDataSource.saveSingleWeeklyRace(weeklyRace);
                        lastUpdateTimestamps.put("last", System.currentTimeMillis());
                        Objects.requireNonNull(raceCache.get("last")).postValue(new Result.NextRaceSuccess(weeklyRace));
                    } else if (isInitialLoad) {
                        Objects.requireNonNull(raceCache.get("last")).postValue(new Result.Error("No last race found from remote"));
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.e(TAG, "Error fetching last race from remote: " + exception.getMessage());
                    if (isInitialLoad) {
                        Objects.requireNonNull(raceCache.get("last")).postValue(new Result.Error(exception.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching last race from remote: " + e.getMessage());
            if (isInitialLoad) {
                Objects.requireNonNull(raceCache.get("last")).postValue(new Result.Error(e.getMessage()));
            }
        }
    }

    private void loadWeeklyRaces() {
        Objects.requireNonNull(raceCache.get("all")).postValue(new Result.Loading("Loading weekly races"));

        localWeeklyRaceDataSource.getWeeklyRaces(new WeeklyRacesCallback() {
            @Override
            public void onSuccess(List<WeeklyRace> weeklyRaces) {
                if (weeklyRaces != null && weeklyRaces.size() >= 10) {
                    lastUpdateTimestamps.put("all", System.currentTimeMillis());
                    Objects.requireNonNull(raceCache.get("all")).postValue(new Result.WeeklyRaceSuccess(weeklyRaces));
                    Log.d(TAG, "Weekly races loaded from local cache");

                    Long ts = lastUpdateTimestamps.get("all");
                    boolean isStale = ts == null || (System.currentTimeMillis() - ts > FRESH_TIMEOUT);
                    if (networkUtils.isConnected() && isStale) {
                        fetchWeeklyRacesFromRemote(false);
                    }
                } else {
                    Log.d(TAG, "Weekly races cache miss or incomplete in local database");
                    if (networkUtils.isConnected()) {
                        fetchWeeklyRacesFromRemote(true);
                    } else if (weeklyRaces != null && !weeklyRaces.isEmpty()) {
                        Objects.requireNonNull(raceCache.get("all")).postValue(new Result.WeeklyRaceSuccess(weeklyRaces));
                    } else {
                        Objects.requireNonNull(raceCache.get("all")).postValue(new Result.Error("Weekly races not found in cache"));
                    }
                }
            }

            @Override
            public void onFailure(Exception exception) {
                if (networkUtils.isConnected()) {
                    fetchWeeklyRacesFromRemote(true);
                } else {
                    Objects.requireNonNull(raceCache.get("all")).postValue(new Result.Error(exception.getMessage()));
                }
            }
        });
    }

    private void fetchWeeklyRacesFromRemote(boolean isInitialLoad) {
        weeklyRaceRemoteDataSource.getWeeklyRaces(new WeeklyRacesCallback() {
            @Override
            public void onSuccess(List<WeeklyRace> weeklyRaces) {
                if (weeklyRaces != null && !weeklyRaces.isEmpty()) {
                    localWeeklyRaceDataSource.saveWeeklyRaces(weeklyRaces);
                    lastUpdateTimestamps.put("all", System.currentTimeMillis());
                    Objects.requireNonNull(raceCache.get("all")).postValue(new Result.WeeklyRaceSuccess(weeklyRaces));
                } else if (isInitialLoad) {
                    loadWeeklyRacesFromLocal();
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error loading weekly races from remote: " + exception.getMessage());
                if (isInitialLoad) {
                    loadWeeklyRacesFromLocal();
                }
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