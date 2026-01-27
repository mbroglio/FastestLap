package com.the_coffe_coders.fastestlap.repository.junior.result;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.source.junior.results.FirebaseJuniorResultsDataSource;
import com.the_coffe_coders.fastestlap.source.junior.results.LocalJuniorResultsDataSource;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

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

        if (!juniorResultCache.containsKey(cacheKey) ||
                !lastUpdateTimestamps.containsKey(cacheKey) ||
                lastUpdateTimestamps.get(cacheKey) == null) {
            juniorResultCache.put(cacheKey, new MutableLiveData<>());
            if (isNetworkAvailable()) {
                loadJuniorResult(series);
            } else {
                fetchFromLocal(cacheKey, series);
            }
        }else if(System.currentTimeMillis() - lastUpdateTimestamps.get(cacheKey) > 60000){
            if (isNetworkAvailable()) {
                loadJuniorResult(series);
            } else {
                fetchFromLocal(cacheKey, series);
            }
        }else{
            Log.i(TAG, "Junior result found in cache: " + cacheKey);
        }
        return juniorResultCache.get(cacheKey);
    }

    private void fetchFromLocal(String cacheKey, String series) {
        Log.i(TAG, "Fetching junior result from local database: " + cacheKey);
        localJuniorResultDataSource.getJuniorResults(series, new JuniorResultCallback() {
            @Override
            public void onResultLoaded(JuniorResult result) {
                if (result != null) {
                    juniorResultCache.put(cacheKey, new MutableLiveData<>(
                            new Result.JuniorResultSuccess(result)));
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(juniorResultCache.get(cacheKey))
                            .postValue(new Result.JuniorResultSuccess(result));
                }else{
                    Log.e(TAG, "Junior result not found in local database");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading junior result from local database: " + e.getMessage());
                Objects.requireNonNull(juniorResultCache.get(cacheKey))
                        .postValue(new Result.Error("Error loading junior result: " + e.getMessage()));
            }
        });
    }

    private void loadJuniorResult(String series) {
        String cacheKey = "juniorResult" + series;
        Log.i(TAG, "Loading junior result from remote: " + cacheKey);
        Objects.requireNonNull(juniorResultCache.get(cacheKey)).postValue(new Result.Loading("Fetching junior result from remote"));

        try{
            firebaseJuniorResultDataSource.getJuniorResults(series, new JuniorResultCallback() {
                @Override
                public void onResultLoaded(JuniorResult result) {
                    Log.i(TAG, "Successfully retrieved junior result from Firebase: " + result);
                    if (result != null) {
                        localJuniorResultDataSource.insertJuniorResult(result);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(juniorResultCache.get(cacheKey))
                                .postValue(new Result.JuniorResultSuccess(result));
                    }else{
                        Log.e(TAG, "Junior result not found");
                        fetchFromLocal(cacheKey, series);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading junior result: " + e.getMessage());
                    localJuniorResultDataSource.deleteJuniorResult(series);
                    Objects.requireNonNull(juniorResultCache.get(cacheKey))
                            .postValue(new Result.Error("Junior result not available yet"));
                }
            });
        }catch (Exception e){
            Log.e(TAG, "Error loading junior result: " + e.getMessage());
            fetchFromLocal(cacheKey, series);
        }
    }

}
