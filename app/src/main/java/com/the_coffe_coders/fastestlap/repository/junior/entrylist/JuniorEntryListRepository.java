package com.the_coffe_coders.fastestlap.repository.junior.entrylist;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorEntryList;
import com.the_coffe_coders.fastestlap.source.junior.entryList.FirebaseJuniorEntryListDataSource;
import com.the_coffe_coders.fastestlap.source.junior.entryList.LocalJuniorEntryListDataSource;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JuniorEntryListRepository {
    private static final String TAG = "JuniorEntryListRepository";
    private static JuniorEntryListRepository instance;

    private final Map<String, MutableLiveData<Result>> juniorEntryListCache;
    private final Map<String, Long> lastUpdateTimestamps;

    private final FirebaseJuniorEntryListDataSource firebaseJuniorEntryListDataSource;
    private final LocalJuniorEntryListDataSource localJuniorEntryListDataSource;

    private final NetworkUtils networkLiveData;

    private JuniorEntryListRepository(AppRoomDatabase appRoomDatabase, Context context) {
        this.juniorEntryListCache = new HashMap<>();
        this.lastUpdateTimestamps = new HashMap<>();
        this.firebaseJuniorEntryListDataSource = FirebaseJuniorEntryListDataSource.getInstance();
        this.localJuniorEntryListDataSource = LocalJuniorEntryListDataSource.getInstance(appRoomDatabase);
        this.networkLiveData = new NetworkUtils(context);
    }

    public static synchronized JuniorEntryListRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new JuniorEntryListRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    }

    public synchronized MutableLiveData<Result> getEntryList(String series) {
        Log.i(TAG, "Fetching junior entry list with series: " + series);
        String cacheKey = "juniorEntryList" + series;

        if (!juniorEntryListCache.containsKey(cacheKey) ||
                !lastUpdateTimestamps.containsKey(cacheKey) ||
                lastUpdateTimestamps.get(cacheKey) == null) {
            juniorEntryListCache.put(cacheKey, new MutableLiveData<>());
            if(isNetworkAvailable()){
                loadJuniorEntryList(series);
            }else{
                fetchFromLocal(cacheKey, series);
            }
        }else if(System.currentTimeMillis() - lastUpdateTimestamps.get(cacheKey) > 60000) {
            if(isNetworkAvailable()){
                loadJuniorEntryList(series);
            }else{
                fetchFromLocal(cacheKey, series);
            }
        }else{
            Log.i(TAG, "Junior entry list found in cache: " + cacheKey);
        }
        return juniorEntryListCache.get(cacheKey);
    }

    private void fetchFromLocal(String cacheKey, String series) {
        Log.i(TAG, "Fetching junior entry list from local database: " + cacheKey);
        localJuniorEntryListDataSource.getJuniorEntryList(series, new JuniorEntryListCallback() {
            @Override
            public void onEntryListLoaded(JuniorEntryList entryList) {
                if (entryList != null) {
                    juniorEntryListCache.put(cacheKey, new MutableLiveData<>(
                            new Result.JuniorEntryListSuccess(entryList)));
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(juniorEntryListCache.get(cacheKey))
                            .postValue(new Result.JuniorEntryListSuccess(entryList));
                }else{
                    Log.e(TAG, "Junior entry list not found in local database");
                    Objects.requireNonNull(juniorEntryListCache.get(cacheKey))
                            .postValue(new Result.Error("Junior entry list not found"));
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading junior entry list from local database: " + e.getMessage());
                Objects.requireNonNull(juniorEntryListCache.get(cacheKey))
                        .postValue(new Result.Error("Error loading junior entry list: " + e.getMessage()));
            }
        });
    }

    private void loadJuniorEntryList(String series) {
        String cacheKey = "juniorEntryList" + series;
        Log.i(TAG, "Loading junior entry list from remote: " + cacheKey);
        Objects.requireNonNull(juniorEntryListCache.get(cacheKey)).postValue(new Result.Loading("Fetching junior entry list from remote"));

        try{
            firebaseJuniorEntryListDataSource.getJuniorEntryList(series, new JuniorEntryListCallback() {
                @Override
                public void onEntryListLoaded(JuniorEntryList entryList) {
                    Log.i(TAG, "Successfully retrieved junior entry list from Firebase: " + entryList);
                    if (entryList != null) {
                        localJuniorEntryListDataSource.insertJuniorEntryList(entryList);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(juniorEntryListCache.get(cacheKey))
                                .postValue(new Result.JuniorEntryListSuccess(entryList));
                    }else{
                        Log.e(TAG, "Junior entry list not found - deleting old data from local DB");
                        localJuniorEntryListDataSource.deleteJuniorEntryList(series);
                        Objects.requireNonNull(juniorEntryListCache.get(cacheKey))
                                .postValue(new Result.Error("Junior entry list not available yet"));
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading junior entry list: " + e.getMessage());
                    fetchFromLocal(cacheKey, series);
                }
            });
        }catch (Exception e){
            Log.e(TAG, "Error loading junior entry list: " + e.getMessage());
            fetchFromLocal(cacheKey, series);
        }
    }


}
