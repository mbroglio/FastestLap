package com.the_coffe_coders.fastestlap.repository.f1.constructor;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.source.f1.constructor.FirebaseConstructorDataSource;
import com.the_coffe_coders.fastestlap.source.f1.constructor.JolpicaConstructorDataSource;
import com.the_coffe_coders.fastestlap.source.f1.constructor.LocalConstructorDataSource;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConstructorRepository {
    private static ConstructorRepository instance;
    private final Map<String, MutableLiveData<Result>> constructorCache;
    private final Map<String, Long> lastUpdateTimestamps;
    private final FirebaseConstructorDataSource firebaseConstructorDataSource;
    private final LocalConstructorDataSource localConstructorDataSource;
    private final String TAG = "ConstructorRepository";

    private final NetworkUtils networkLiveData;

    private ConstructorRepository(AppRoomDatabase appRoomDatabase, Context context) {
        constructorCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        firebaseConstructorDataSource = FirebaseConstructorDataSource.getInstance();
        JolpicaConstructorDataSource.getInstance();
        localConstructorDataSource = LocalConstructorDataSource.getInstance(appRoomDatabase);
        networkLiveData = new NetworkUtils(context);
    }

    public static ConstructorRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new ConstructorRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    }

    public synchronized MutableLiveData<Result> getConstructor(String constructorId) {
        if (!constructorCache.containsKey(constructorId)) {
            constructorCache.put(constructorId, new MutableLiveData<>());
            loadConstructorCacheFirst(constructorId);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(constructorId);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) {
                if (isNetworkAvailable()) {
                    loadConstructorFromRemote(constructorId, true);
                }
            } else {
                Log.i(TAG, "Constructor found in cache: " + constructorId);
            }
        }
        return constructorCache.get(constructorId);
    }

    private void loadConstructorCacheFirst(String constructorId) {
        localConstructorDataSource.getConstructor(constructorId, new ConstructorCallback() {
            @Override
            public void onConstructorLoaded(Constructor constructor) {
                if (constructor != null) {
                    Log.d(TAG, "Constructor loaded from local database (cache hit): " + constructorId);
                    constructor.setConstructorId(constructorId);
                    lastUpdateTimestamps.put(constructorId, System.currentTimeMillis());
                    Objects.requireNonNull(constructorCache.get(constructorId)).postValue(new Result.ConstructorSuccess(constructor));

                    // Only refresh from remote if the cached data is actually stale.
                    // Without this TTL guard, Firebase fires on every launch even when the
                    // local data is fresh, causing the LiveData to re-emit and triggering
                    // redundant card rebuilds in the UI.
                    Long ts = lastUpdateTimestamps.get(constructorId);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300_000L;
                    if (isNetworkAvailable() && isStale) {
                        loadConstructorFromRemote(constructorId, true);
                    } else {
                        Log.d(TAG, "Constructor cache still fresh, skipping remote refresh: " + constructorId);
                    }
                } else {
                    Log.d(TAG, "Constructor cache miss in local database: " + constructorId);
                    if (isNetworkAvailable()) {
                        loadConstructorFromRemote(constructorId, false);
                    } else {
                        Objects.requireNonNull(constructorCache.get(constructorId)).postValue(
                                new Result.Error("Constructor not found locally and no network connection available"));
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking local database for constructor: " + e.getMessage());
                if (isNetworkAvailable()) {
                    loadConstructorFromRemote(constructorId, false);
                }
            }
        });
    }

    private void loadConstructorFromRemote(String constructorId, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            constructorCache.get(constructorId).postValue(new Result.Loading("Fetching constructor from remote"));
        }
        try {
            firebaseConstructorDataSource.getConstructor(constructorId, new ConstructorCallback() {
                @Override
                public void onConstructorLoaded(Constructor constructor) {
                    if (constructor != null) {
                        constructor.setConstructorId(constructorId);
                        localConstructorDataSource.insertConstructor(constructor);
                        lastUpdateTimestamps.put(constructorId, System.currentTimeMillis());
                        Objects.requireNonNull(constructorCache.get(constructorId)).postValue(new Result.ConstructorSuccess(constructor));
                    } else if (!isBackgroundRefresh) {
                        Log.e(TAG, "Constructor not found: " + constructorId);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading constructor from remote: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading constructor from remote: " + e.getMessage());
        }
    }
}
