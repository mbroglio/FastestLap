package com.the_coffe_coders.fastestlap.repository.junior.calendar;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.source.junior.calendar.FirebaseJuniorCalendarDataSource;
import com.the_coffe_coders.fastestlap.source.junior.calendar.LocalJuniorCalendarDataSource;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;

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

        if (!juniorCalendarCache.containsKey(cacheKey)) {
            juniorCalendarCache.put(cacheKey, new MutableLiveData<>());
            loadJuniorCalendarCacheFirst(cacheKey, series);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(cacheKey);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) {
                if (isNetworkAvailable()) {
                    loadJuniorCalendar(series, true);
                }
            } else {
                Log.i(TAG, "Junior calendar found in cache: " + cacheKey);
            }
        }

        return juniorCalendarCache.get(cacheKey);
    }

    private void loadJuniorCalendarCacheFirst(String cacheKey, String series) {
        localJuniorCalendarDataSource.getJuniorCalendar(series, new JuniorCalendarCallback() {
            @Override
            public void onCalendarLoaded(JuniorCalendar calendar) {
                if (calendar != null) {
                    Log.i(TAG, "Junior calendar loaded from local DB: " + cacheKey);
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(juniorCalendarCache.get(cacheKey))
                            .postValue(new Result.JuniorCalendarSuccess(calendar));

                    Long ts = lastUpdateTimestamps.get(cacheKey);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300_000L;
                    if (isNetworkAvailable() && isStale) {
                        loadJuniorCalendar(series, true);
                    }
                } else {
                    Log.i(TAG, "Junior calendar cache miss in local DB");
                    if (isNetworkAvailable()) {
                        loadJuniorCalendar(series, false);
                    } else {
                        Objects.requireNonNull(juniorCalendarCache.get(cacheKey))
                                .postValue(new Result.Error("Junior calendar not available offline"));
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading junior calendar from local DB: " + e.getMessage());
                if (isNetworkAvailable()) {
                    loadJuniorCalendar(series, false);
                }
            }
        });
    }

    private void loadJuniorCalendar(String series, boolean isBackgroundRefresh) {
        String cacheKey = "juniorCalendar" + series;
        Log.i(TAG, "Loading junior calendar from remote: " + cacheKey);
        if (!isBackgroundRefresh) {
            Objects.requireNonNull(juniorCalendarCache.get(cacheKey)).postValue(new Result.Loading("Fetching junior calendar from remote"));
        }

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
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading junior calendar: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading junior calendar: " + e.getMessage());
        }
    }
}
