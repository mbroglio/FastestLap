package com.the_coffe_coders.fastestlap.repository.standing.constructor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.source.standing.constructor.JolpicaConstructorStandingsDataSource;
import com.the_coffe_coders.fastestlap.source.standing.constructor.LocalConstructorStandingsDataSource;

import java.util.HashMap;
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

    private final Context context;

    private ConstructorStandingRepository(AppRoomDatabase appRoomDatabase, Context context) {
        constructorStandingCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        this.jolpicaConstructorStandingsDataSource = JolpicaConstructorStandingsDataSource.getInstance();
        this.localConstructorStandingsDataSource = LocalConstructorStandingsDataSource.getInstance(appRoomDatabase);
        this.context = context;
    }

    public static synchronized ConstructorStandingRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new ConstructorStandingRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    public synchronized MutableLiveData<Result> getConstructorStandings() {
        Log.d(TAG, "Fetching constructor standing");
        String cacheKey = "constructorStanding";

        if (!constructorStandingCache.containsKey(cacheKey) ||
                !lastUpdateTimestamps.containsKey(cacheKey) ||
                lastUpdateTimestamps.get(cacheKey) == null) {
            constructorStandingCache.put(cacheKey, new MutableLiveData<>());
            loadConstructorStanding();
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(cacheKey) > 60000) {
            if(isNetworkAvailable())
                loadConstructorStanding();
            else {
                fetchFromLocal(cacheKey);
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
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading constructor standing: " + e.getMessage());
                    fetchFromLocal(cacheKey);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading constructor standing: " + e.getMessage());
            fetchFromLocal(cacheKey);
        }
    }

    private void fetchFromLocal(String cacheKey) {
        localConstructorStandingsDataSource.getConstructorStandings(new ConstructorStandingCallback() {
            @Override
            public void onConstructorLoaded(ConstructorStandings constructorStandings) {
                if (constructorStandings != null) {
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
            public void onError(Exception e) {
                Log.e(TAG, "Error loading constructor standing from local database: " + e.getMessage());
                Objects.requireNonNull(constructorStandingCache.get(cacheKey))
                        .postValue(new Result.Error("Error loading constructor standing: " + e.getMessage()));
            }
        });
    }
}