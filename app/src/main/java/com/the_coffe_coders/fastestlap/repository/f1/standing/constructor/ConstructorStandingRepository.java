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
    // Prevents duplicate concurrent HTTP requests while a fetch is already in-flight.
    // Without this, callers that arrive before the first response lands would each fire
    // their own network request (since the timestamp is null until the callback completes).
    private boolean isFetchInFlight = false;

    // Data sources
    private final JolpicaConstructorStandingsDataSource jolpicaConstructorStandingsDataSource;
    private final LocalConstructorStandingsDataSource localConstructorStandingsDataSource;

    private final NetworkUtils networkLiveData;

    private final String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

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
            // First call ever: create the LiveData and start the fetch.
            constructorStandingCache.put(cacheKey, new MutableLiveData<>());
            lastUpdateTimestamps.remove(cacheKey);
        }

        if (!lastUpdateTimestamps.containsKey(cacheKey) || lastUpdateTimestamps.get(cacheKey) == null) {
            // No successful result yet. Only start a fetch if one isn't already in-flight.
            if (!isFetchInFlight) {
                isFetchInFlight = true;
                if (isNetworkAvailable()) {
                    loadConstructorStanding();
                } else {
                    fetchFromLocal(cacheKey);
                }
            } else {
                Log.d(TAG, "Constructor standing fetch already in-flight, returning shared LiveData");
            }
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(cacheKey) > 60000) {
            // Cache is stale — refresh.
            if (!isFetchInFlight) {
                isFetchInFlight = true;
                if (isNetworkAvailable())
                    loadConstructorStanding();
                else {
                    fetchFromLocal(cacheKey);
                }
            }
        } else {
            Log.d(TAG, "Constructor standing found in cache");
        }
        return constructorStandingCache.get(cacheKey);
    }

    private void loadConstructorStanding() {
        String cacheKey = "constructorStanding";
        Objects.requireNonNull(constructorStandingCache.get(cacheKey)).postValue(new Result.Loading("Fetching constructor standing from remote"));
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
                    } else {
                        Log.e(TAG, "Constructor standing not found");
                        fetchFromLocal(cacheKey);
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
                    } else {
                        Log.e(TAG, "Constructor list not found");
                        fetchFromLocal(cacheKey);
                    }
                }

                @Override
                public void onError(Exception e) {
                    isFetchInFlight = false;
                    Log.e(TAG, "Error loading constructor standing: " + e.getMessage());
                    fetchFromLocal(cacheKey);
                }
            });
        } catch (Exception e) {
            isFetchInFlight = false;
            Log.e(TAG, "Error loading constructor standing: " + e.getMessage());
            fetchFromLocal(cacheKey);
        }
    }

    private void fetchFromLocal(String cacheKey) {
        localConstructorStandingsDataSource.getConstructorStandings(new ConstructorStandingCallback() {
            @Override
            public void onConstructorLoaded(ConstructorStandings constructorStandings) {
                if (constructorStandings != null && constructorStandings.getSeason().equals(currentYear)) {
                    constructorStandingCache.put(cacheKey, new MutableLiveData<>(
                            new Result.ConstructorStandingsSuccess(constructorStandings)));
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                            .postValue(new Result.ConstructorStandingsSuccess(constructorStandings));
                } else {
                    Log.e(TAG, "Constructor standing not found in local database");
                    Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                            .postValue(new Result.Error("Constructor standing not found"));
                }
            }

            @Override
            public void onConstructorListLoaded(List<Constructor> constructorList) {
                if (constructorList != null) {
                    constructorStandingCache.put(cacheKey, new MutableLiveData<>(
                            new Result.ConstructorsSuccess(constructorList)));
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                            .postValue(new Result.ConstructorsSuccess(constructorList));
                } else {
                    Log.e(TAG, "Constructor list not found in local database");
                    Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                            .postValue(new Result.Error("Constructor list not found"));
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading constructor standing from local database: " + e.getMessage());
                Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                        .postValue(new Result.Error("Error loading constructor standing: " + e.getMessage()));
            }
        });
    }
}