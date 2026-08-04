package com.the_coffe_coders.fastestlap.repository.junior.result;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.source.junior.results.FirebaseJuniorResultsDataSource;
import com.the_coffe_coders.fastestlap.source.junior.results.LocalJuniorResultsDataSource;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JuniorResultRepository {
    private static final String TAG = "JuniorResultRepository";
    private static JuniorResultRepository instance;

    private final Map<String, MutableLiveData<Result>> juniorResultCache;
    private final Map<String, Long> lastUpdateTimestamps;

    private final FirebaseJuniorResultsDataSource firebaseJuniorResultDataSource;
    private final LocalJuniorResultsDataSource localJuniorResultDataSource;

    private final NetworkUtils networkLiveData;

    private JuniorResultRepository(AppRoomDatabase appRoomDatabase, Context context) {
        this.juniorResultCache = new HashMap<>();
        this.lastUpdateTimestamps = new HashMap<>();
        this.firebaseJuniorResultDataSource = FirebaseJuniorResultsDataSource.getInstance();
        this.localJuniorResultDataSource = LocalJuniorResultsDataSource.getInstance(appRoomDatabase);
        this.networkLiveData = new NetworkUtils(context);
    }

    public static synchronized JuniorResultRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new JuniorResultRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    }

    public synchronized MutableLiveData<Result> getResults(String series) {
        String cacheKey = "juniorResult" + series;

        if (!juniorResultCache.containsKey(cacheKey)) {
            juniorResultCache.put(cacheKey, new MutableLiveData<>());
            loadJuniorResultCacheFirst(cacheKey, series);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(cacheKey);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) {
                if (isNetworkAvailable()) {
                    loadJuniorResult(series, true);
                }
            } else {
                Log.i(TAG, "Junior result found in cache: " + cacheKey);
            }
        }
        return juniorResultCache.get(cacheKey);
    }

    private void loadJuniorResultCacheFirst(String cacheKey, String series) {
        localJuniorResultDataSource.getJuniorResults(series, new JuniorResultCallback() {
            @Override
            public void onResultLoaded(JuniorResult result) {
                if (result != null) {
                    Log.i(TAG, "Junior result loaded from local DB: " + cacheKey);
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(juniorResultCache.get(cacheKey))
                            .postValue(new Result.JuniorResultSuccess(result));

                    Long ts = lastUpdateTimestamps.get(cacheKey);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300_000L;
                    if (isNetworkAvailable() && isStale) {
                        loadJuniorResult(series, true);
                    }
                } else {
                    Log.i(TAG, "Junior result cache miss in local DB");
                    if (isNetworkAvailable()) {
                        loadJuniorResult(series, false);
                    } else {
                        Objects.requireNonNull(juniorResultCache.get(cacheKey))
                                .postValue(new Result.Error("Junior result not available offline"));
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading junior result from local DB: " + e.getMessage());
                if (isNetworkAvailable()) {
                    loadJuniorResult(series, false);
                }
            }
        });
    }

    private void loadJuniorResult(String series, boolean isBackgroundRefresh) {
        String cacheKey = "juniorResult" + series;
        Log.i(TAG, "Loading junior result from remote: " + cacheKey);
        if (!isBackgroundRefresh) {
            Objects.requireNonNull(juniorResultCache.get(cacheKey)).postValue(new Result.Loading("Fetching junior result from remote"));
        }

        try {
            firebaseJuniorResultDataSource.getJuniorResults(series, new JuniorResultCallback() {
                @Override
                public void onResultLoaded(JuniorResult result) {
                    Log.i(TAG, "Successfully retrieved junior result from Firebase: " + result);
                    if (result != null) {
                        localJuniorResultDataSource.insertJuniorResult(result);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(juniorResultCache.get(cacheKey))
                                .postValue(new Result.JuniorResultSuccess(result));
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading junior result: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading junior result: " + e.getMessage());
        }
    }

}
