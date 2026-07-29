package com.the_coffe_coders.fastestlap.repository.f1.standing.constructor;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;
import com.the_coffe_coders.fastestlap.source.f1.standing.constructor.JolpicaConstructorStandingsDataSource;
import com.the_coffe_coders.fastestlap.source.f1.standing.constructor.LocalConstructorStandingsDataSource;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConstructorStandingRepository {
    private static final String TAG = "ConstructorStandingRepository";
    private static ConstructorStandingRepository instance;

    // Cache
    private final Map<String, MutableLiveData<Result>> constructorStandingCache;
    private final Map<String, Long> lastUpdateTimestamps;
    // Data sources
    private final JolpicaConstructorStandingsDataSource jolpicaConstructorStandingsDataSource;
    private final LocalConstructorStandingsDataSource localConstructorStandingsDataSource;
    private final NetworkUtils networkLiveData;
    private final String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    // Prevents duplicate concurrent HTTP requests while a fetch is already in-flight.
    // Without this, callers that arrive before the first response lands would each fire
    // their own network request (since the timestamp is null until the callback completes).
    private boolean isFetchInFlight = false;

    private ConstructorStandingRepository(AppRoomDatabase appRoomDatabase, Context context) {
        constructorStandingCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        this.jolpicaConstructorStandingsDataSource = JolpicaConstructorStandingsDataSource.getInstance();
        this.localConstructorStandingsDataSource = LocalConstructorStandingsDataSource.getInstance(appRoomDatabase);
        this.networkLiveData = new NetworkUtils(context);
    }

    public static synchronized ConstructorStandingRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new ConstructorStandingRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    }

    public synchronized MutableLiveData<Result> getConstructorStandings() {
        Log.d(TAG, "Fetching constructor standing");
        String cacheKey = "constructorStanding";

        if (!constructorStandingCache.containsKey(cacheKey)) {
            constructorStandingCache.put(cacheKey, new MutableLiveData<>());
            loadConstructorStandingsCacheFirst(cacheKey);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(cacheKey);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) { // 5 min TTL
                if (isNetworkAvailable() && !isFetchInFlight) {
                    isFetchInFlight = true;
                    loadConstructorStandingFromRemote(cacheKey, true);
                }
            } else {
                Log.d(TAG, "Constructor standing found in cache");
            }
        }
        return constructorStandingCache.get(cacheKey);
    }

    private void loadConstructorStandingsCacheFirst(String cacheKey) {
        localConstructorStandingsDataSource.getConstructorStandings(new ConstructorStandingCallback() {
            @Override
            public void onConstructorLoaded(ConstructorStandings constructorStandings) {
                if (constructorStandings != null && currentYear.equals(constructorStandings.getSeason())) {
                    Log.d(TAG, "Constructor standings loaded from local database (cache hit)");
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                            .postValue(new Result.ConstructorStandingsSuccess(constructorStandings));

                    Long ts = lastUpdateTimestamps.get(cacheKey);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300_000L;
                    if (isNetworkAvailable() && !isFetchInFlight && isStale) {
                        isFetchInFlight = true;
                        loadConstructorStandingFromRemote(cacheKey, true);
                    }
                } else {
                    Log.d(TAG, "Constructor standings cache miss in local database");
                    if (isNetworkAvailable() && !isFetchInFlight) {
                        isFetchInFlight = true;
                        loadConstructorStandingFromRemote(cacheKey, false);
                    } else if (!isFetchInFlight) {
                        Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                                .postValue(new Result.Error("Constructor standing not found locally and no network connection"));
                    }
                }
            }

            @Override
            public void onConstructorListLoaded(List<Constructor> constructorList) {
                Log.d(TAG, "Constructor list only in local DB — fetching full standings from remote");
                if (isNetworkAvailable() && !isFetchInFlight) {
                    isFetchInFlight = true;
                    loadConstructorStandingFromRemote(cacheKey, false);
                } else if (!isFetchInFlight) {
                    Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                            .postValue(new Result.Error("Constructor standing not found locally and no network connection"));
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking local database for standings: " + e.getMessage());
                if (isNetworkAvailable() && !isFetchInFlight) {
                    isFetchInFlight = true;
                    loadConstructorStandingFromRemote(cacheKey, false);
                }
            }
        });
    }

    private void loadConstructorStandingFromRemote(String cacheKey, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                    .postValue(new Result.Loading("Fetching constructor standing from remote"));
        }
        try {
            jolpicaConstructorStandingsDataSource.getConstructorStandings(new ConstructorStandingCallback() {
                @Override
                public void onConstructorLoaded(ConstructorStandings constructorStandings) {
                    isFetchInFlight = false;
                    if (constructorStandings != null) {
                        localConstructorStandingsDataSource.insertConstructorStandings(constructorStandings);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                                .postValue(new Result.ConstructorStandingsSuccess(constructorStandings));
                    }
                }

                @Override
                public void onConstructorListLoaded(List<Constructor> constructorList) {
                    isFetchInFlight = false;
                    if (constructorList != null) {
                        localConstructorStandingsDataSource.insertConstructorList(constructorList);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                                .postValue(new Result.ConstructorsSuccess(constructorList));
                    }
                }

                @Override
                public void onError(Exception e) {
                    isFetchInFlight = false;
                    Log.e(TAG, "Error loading constructor standing from remote: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            isFetchInFlight = false;
            Log.e(TAG, "Error loading constructor standing from remote: " + e.getMessage());
        }
    }
}