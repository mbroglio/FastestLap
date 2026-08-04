package com.the_coffe_coders.fastestlap.repository.junior.standings;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandings;
import com.the_coffe_coders.fastestlap.source.junior.standings.FirebaseJuniorStandingsDataSource;
import com.the_coffe_coders.fastestlap.source.junior.standings.LocalJuniorStandingsDataSource;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JuniorStandingsRepository {
    private static final String TAG = "JuniorStandingsRepository";
    private static JuniorStandingsRepository instance;

    private final Map<String, MutableLiveData<Result>> juniorStandingsCache;
    private final Map<String, Long> lastUpdateTimestamps;

    private final FirebaseJuniorStandingsDataSource firebaseJuniorStandingsDataSource;
    private final LocalJuniorStandingsDataSource localJuniorStandingsDataSource;

    private final NetworkUtils networkLiveData;

    private JuniorStandingsRepository(AppRoomDatabase appRoomDatabase, Context context) {
        this.juniorStandingsCache = new HashMap<>();
        this.lastUpdateTimestamps = new HashMap<>();
        this.firebaseJuniorStandingsDataSource = FirebaseJuniorStandingsDataSource.getInstance();
        this.localJuniorStandingsDataSource = LocalJuniorStandingsDataSource.getInstance(appRoomDatabase);
        this.networkLiveData = new NetworkUtils(context);
    }

    public static synchronized JuniorStandingsRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new JuniorStandingsRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    }

    public MutableLiveData<Result> getConstructorStandings(String series) {
        String cacheKey = "juniorConstructorStandings" + series;

        if (!juniorStandingsCache.containsKey(cacheKey)) {
            juniorStandingsCache.put(cacheKey, new MutableLiveData<>());
            loadConstructorStandingsCacheFirst(cacheKey, series);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(cacheKey);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) { // 5 min TTL
                if (isNetworkAvailable()) {
                    loadConstructorStandingsFromRemote(cacheKey, series, true);
                }
            } else {
                Log.i(TAG, "Junior constructor standings found in cache: " + cacheKey);
            }
        }
        return juniorStandingsCache.get(cacheKey);
    }

    private void loadConstructorStandingsCacheFirst(String cacheKey, String series) {
        localJuniorStandingsDataSource.getJuniorConstructorStandings(series, new JuniorStandingsCallback() {
            @Override
            public void onDriverStandingsLoaded(JuniorDriverStandings driverStandings) {
            }

            @Override
            public void onConstructorStandingsLoaded(JuniorConstructorStandings constructorStandings) {
                if (constructorStandings != null) {
                    Log.i(TAG, "Junior constructor standings loaded from local DB: " + cacheKey);
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                            .postValue(new Result.JuniorConstructorStandingsSuccess(constructorStandings));

                    if (isNetworkAvailable()) {
                        loadConstructorStandingsFromRemote(cacheKey, series, true);
                    }
                } else {
                    Log.i(TAG, "Junior constructor standings cache miss in local DB");
                    if (isNetworkAvailable()) {
                        loadConstructorStandingsFromRemote(cacheKey, series, false);
                    } else {
                        Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                                .postValue(new Result.Error("Junior constructor standings not available offline"));
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error reading local DB for junior constructor standings: " + e.getMessage());
                if (isNetworkAvailable()) {
                    loadConstructorStandingsFromRemote(cacheKey, series, false);
                }
            }
        });
    }

    private void loadConstructorStandingsFromRemote(String cacheKey, String series, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            Objects.requireNonNull(juniorStandingsCache.get(cacheKey)).postValue(new Result.Loading("Fetching junior constructor standings from remote"));
        }
        try {
            firebaseJuniorStandingsDataSource.getJuniorConstructorStandings(series, new JuniorStandingsCallback() {
                @Override
                public void onDriverStandingsLoaded(JuniorDriverStandings driverStandings) {
                }

                @Override
                public void onConstructorStandingsLoaded(JuniorConstructorStandings constructorStandings) {
                    if (constructorStandings != null) {
                        localJuniorStandingsDataSource.insertJuniorConstructorStandings(constructorStandings);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                                .postValue(new Result.JuniorConstructorStandingsSuccess(constructorStandings));
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading junior constructor standings from remote: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading junior constructor standings from remote: " + e.getMessage());
        }
    }

    public MutableLiveData<Result> getDriverStandings(String series) {
        String cacheKey = "juniorDriverStandings" + series;

        if (!juniorStandingsCache.containsKey(cacheKey)) {
            juniorStandingsCache.put(cacheKey, new MutableLiveData<>());
            loadDriverStandingsCacheFirst(cacheKey, series);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(cacheKey);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) { // 5 min TTL
                if (isNetworkAvailable()) {
                    loadDriverStandingsFromRemote(cacheKey, series, true);
                }
            } else {
                Log.i(TAG, "Junior driver standings found in cache: " + cacheKey);
            }
        }
        return juniorStandingsCache.get(cacheKey);
    }

    private void loadDriverStandingsCacheFirst(String cacheKey, String series) {
        localJuniorStandingsDataSource.getJuniorDriverStandings(series, new JuniorStandingsCallback() {
            @Override
            public void onDriverStandingsLoaded(JuniorDriverStandings driverStandings) {
                if (driverStandings != null) {
                    Log.i(TAG, "Junior driver standings loaded from local DB: " + cacheKey);
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                            .postValue(new Result.JuniorDriverStandingsSuccess(driverStandings));

                    if (isNetworkAvailable()) {
                        loadDriverStandingsFromRemote(cacheKey, series, true);
                    }
                } else {
                    Log.i(TAG, "Junior driver standings cache miss in local DB");
                    if (isNetworkAvailable()) {
                        loadDriverStandingsFromRemote(cacheKey, series, false);
                    } else {
                        Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                                .postValue(new Result.Error("Junior driver standings not available offline"));
                    }
                }
            }

            @Override
            public void onConstructorStandingsLoaded(JuniorConstructorStandings constructorStandings) {
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error reading local DB for junior driver standings: " + e.getMessage());
                if (isNetworkAvailable()) {
                    loadDriverStandingsFromRemote(cacheKey, series, false);
                }
            }
        });
    }

    private void loadDriverStandingsFromRemote(String cacheKey, String series, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            Objects.requireNonNull(juniorStandingsCache.get(cacheKey)).postValue(new Result.Loading("Fetching junior driver standings from remote"));
        }
        try {
            firebaseJuniorStandingsDataSource.getJuniorDriverStandings(series, new JuniorStandingsCallback() {
                @Override
                public void onDriverStandingsLoaded(JuniorDriverStandings driverStandings) {
                    if (driverStandings != null) {
                        localJuniorStandingsDataSource.insertJuniorDriverStandings(driverStandings);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                                .postValue(new Result.JuniorDriverStandingsSuccess(driverStandings));
                    }
                }

                @Override
                public void onConstructorStandingsLoaded(JuniorConstructorStandings constructorStandings) {
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading junior driver standings from remote: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading junior driver standings from remote: " + e.getMessage());
        }
    }
}
