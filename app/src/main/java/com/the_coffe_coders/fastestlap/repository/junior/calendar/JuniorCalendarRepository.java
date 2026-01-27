package com.the_coffe_coders.fastestlap.repository.junior.calendar;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.source.junior.calendar.FirebaseJuniorCalendarDataSource;
import com.the_coffe_coders.fastestlap.source.junior.calendar.LocalJuniorCalendarDataSource;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JuniorCalendarRepository {
    private static final String TAG = "JuniorCalendarRepository";
    private static JuniorCalendarRepository instance;

    private final Map<String, MutableLiveData<Result>> juniorCalendarCache;
    private final Map<String, Long> lastUpdateTimestamps;


    private final FirebaseJuniorCalendarDataSource firebaseJuniorCalendarDataSource;
    private final LocalJuniorCalendarDataSource localJuniorCalendarDataSource;

    private final NetworkUtils networkLiveData;


    private JuniorCalendarRepository(AppRoomDatabase appRoomDatabase, Context context) {
        this.juniorCalendarCache = new HashMap<>();
        this.lastUpdateTimestamps = new HashMap<>();
        this.firebaseJuniorCalendarDataSource = FirebaseJuniorCalendarDataSource.getInstance();
        this.localJuniorCalendarDataSource = LocalJuniorCalendarDataSource.getInstance(appRoomDatabase);
        this.networkLiveData = new NetworkUtils(context);
    }

    public static synchronized JuniorCalendarRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new JuniorCalendarRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    }

    public synchronized MutableLiveData<Result> getCalendar(String series) {
        Log.i(TAG, "Fetching junior calendar with series: " + series);
        String cacheKey = "juniorCalendar" + series;

        if (!juniorCalendarCache.containsKey(cacheKey) ||
                !lastUpdateTimestamps.containsKey(cacheKey) ||
                lastUpdateTimestamps.get(cacheKey) == null) {
            juniorCalendarCache.put(cacheKey, new MutableLiveData<>());
            if (isNetworkAvailable()) {
                loadJuniorCalendar(series);
            } else {
                fetchFromLocal(cacheKey, series);
            }
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(cacheKey) > 60000) {
            if (isNetworkAvailable()) {
                loadJuniorCalendar(series);
            } else {
                fetchFromLocal(cacheKey, series);
            }
        } else {
            Log.i(TAG, "Junior calendar found in cache: " + cacheKey);
        }

        return juniorCalendarCache.get(cacheKey);
    }

    private void fetchFromLocal(String cacheKey, String series) {
        Log.i(TAG, "Fetching junior calendar from local database: " + cacheKey);
        localJuniorCalendarDataSource.getJuniorCalendar(series, new JuniorCalendarCallback() {
            @Override
            public void onCalendarLoaded(JuniorCalendar calendar) {
                if (calendar != null) {
                    juniorCalendarCache.put(cacheKey, new MutableLiveData<>(
                            new Result.JuniorCalendarSuccess(calendar)));
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(juniorCalendarCache.get(cacheKey))
                            .postValue(new Result.JuniorCalendarSuccess(calendar));
                } else {
                    Log.e(TAG, "Junior calendar not found in local database");
                    Objects.requireNonNull(juniorCalendarCache.get(cacheKey))
                            .postValue(new Result.Error("Junior calendar not found"));
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading junior calendar from local database: " + e.getMessage());
                Objects.requireNonNull(juniorCalendarCache.get(cacheKey))
                        .postValue(new Result.Error("Error loading junior calendar: " + e.getMessage()));
            }
        });

    }

    private void loadJuniorCalendar(String series) {
        String cacheKey = "juniorCalendar" + series;
        Log.i(TAG, "Loading junior calendar from remote: " + cacheKey);
        Objects.requireNonNull(juniorCalendarCache.get(cacheKey)).postValue(new Result.Loading("Fetching junior calendar from remote"));

        try {
            firebaseJuniorCalendarDataSource.getJuniorCalendar(series, new JuniorCalendarCallback() {
                @Override
                public void onCalendarLoaded(JuniorCalendar calendar) {
                    Log.i(TAG, "Successfully retrieved junior calendar from Firebase: " + calendar);
                    if (calendar != null) {
                        localJuniorCalendarDataSource.insertJuniorCalendar(calendar);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(juniorCalendarCache.get(cacheKey))
                                .postValue(new Result.JuniorCalendarSuccess(calendar));
                    } else {
                        Log.e(TAG, "Junior calendar not found");
                        fetchFromLocal(cacheKey, series);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading junior calendar: " + e.getMessage());
                    localJuniorCalendarDataSource.deleteJuniorCalendar(series);
                    Objects.requireNonNull(juniorCalendarCache.get(cacheKey))
                            .postValue(new Result.Error("Junior calendar not available yet"));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading junior calendar: " + e.getMessage());
            fetchFromLocal(cacheKey, series);
        }
    }
}
