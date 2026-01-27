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
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

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

        if (!juniorStandingsCache.containsKey(cacheKey) ||
                !lastUpdateTimestamps.containsKey(cacheKey) ||
                lastUpdateTimestamps.get(cacheKey) == null) {
            juniorStandingsCache.put(cacheKey, new MutableLiveData<>());
            if (isNetworkAvailable()) {
                loadConstructorStandings(series);
            } else {
                fetchFromLocalConstructors(cacheKey, series);
            }

        }else if(System.currentTimeMillis() - lastUpdateTimestamps.get(cacheKey) > 60000){
            if (isNetworkAvailable()) {
                loadConstructorStandings(series);
            }else{
                fetchFromLocalConstructors(cacheKey, series);
            }
        }else{
            Log.i(TAG, "Junior constructor standings found in cache: " + cacheKey);
        }
        return juniorStandingsCache.get(cacheKey);

    }

    private void loadConstructorStandings(String series) {
        String cacheKey = "juniorConstructorStandings" + series;
        Log.i(TAG, "Loading junior constructor standings from remote: " + cacheKey);
        Objects.requireNonNull(juniorStandingsCache.get(cacheKey)).postValue(new Result.Loading("Fetching junior constructor standings from remote"));

        try{
            firebaseJuniorStandingsDataSource.getJuniorConstructorStandings(series, new JuniorStandingsCallback() {

                @Override
                public void onDriverStandingsLoaded(JuniorDriverStandings driverStandings) {

                }

                @Override
                public void onConstructorStandingsLoaded(JuniorConstructorStandings constructorStandings) {
                    Log.i(TAG, "Successfully retrieved junior constructor standings from Firebase: " + constructorStandings);
                    if (constructorStandings != null) {
                        localJuniorStandingsDataSource.insertJuniorConstructorStandings(constructorStandings);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                                .postValue(new Result.JuniorConstructorStandingsSuccess(constructorStandings));
                    }else{
                        Log.e(TAG, "Junior constructor standings not found");
                        fetchFromLocalConstructors(cacheKey, series);
                    }

                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading junior constructor standings: " + e.getMessage());
                    localJuniorStandingsDataSource.deleteJuniorConstructorStandings(series);
                    Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                            .postValue(new Result.Error("Junior constructor standings not available yet"));
                }
            });
        }catch (Exception e){
            Log.e(TAG, "Error loading junior constructor standings: " + e.getMessage());
            fetchFromLocalConstructors(cacheKey, series);
        }
    }


    private void fetchFromLocalConstructors(String cacheKey, String series) {
        Log.i(TAG, "Fetching junior constructor standings from local database: " + cacheKey);
        localJuniorStandingsDataSource.getJuniorConstructorStandings(series, new JuniorStandingsCallback() {

            @Override
            public void onDriverStandingsLoaded(JuniorDriverStandings driverStandings) {

            }

            @Override
            public void onConstructorStandingsLoaded(JuniorConstructorStandings constructorStandings) {
                if (constructorStandings != null) {
                    juniorStandingsCache.put(cacheKey, new MutableLiveData<>(
                            new Result.JuniorConstructorStandingsSuccess(constructorStandings)));
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                            .postValue(new Result.JuniorConstructorStandingsSuccess(constructorStandings));
                }else{
                    Log.e(TAG, "Junior constructor standings not found in local database");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading junior constructor standings from local database: " + e.getMessage());
                Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                        .postValue(new Result.Error("Error loading junior constructor standings: " + e.getMessage()));
            }
        });
    }

    public MutableLiveData<Result> getDriverStandings(String series) {
        String cacheKey = "juniorDriverStandings" + series;

        if (!juniorStandingsCache.containsKey(cacheKey) ||
                !lastUpdateTimestamps.containsKey(cacheKey) ||
                lastUpdateTimestamps.get(cacheKey) == null) {
            juniorStandingsCache.put(cacheKey, new MutableLiveData<>());
            if (isNetworkAvailable()) {
                loadDriverStandings(series);
            }else{
                fetchFromLocalDrivers(cacheKey, series);
            }
        }else if(System.currentTimeMillis() - lastUpdateTimestamps.get(cacheKey) > 60000){
            if (isNetworkAvailable()) {
                loadDriverStandings(series);
            }else{
                fetchFromLocalDrivers(cacheKey, series);
            }
        }else{
            Log.i(TAG, "Junior driver standings found in cache: " + cacheKey);
        }
        return juniorStandingsCache.get(cacheKey);
    }

    private void loadDriverStandings(String series) {
        String cacheKey = "juniorDriverStandings" + series;
        Log.i(TAG, "Loading junior driver standings from remote: " + cacheKey);
        Objects.requireNonNull(juniorStandingsCache.get(cacheKey)).postValue(new Result.Loading("Fetching junior driver standings from remote"));

        try{
            firebaseJuniorStandingsDataSource.getJuniorDriverStandings(series, new JuniorStandingsCallback() {

                @Override
                public void onDriverStandingsLoaded(JuniorDriverStandings driverStandings) {
                    Log.i(TAG, "Successfully retrieved junior driver standings from Firebase: " + driverStandings);
                    if (driverStandings != null){
                        localJuniorStandingsDataSource.insertJuniorDriverStandings(driverStandings);
                        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                        Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                                .postValue(new Result.JuniorDriverStandingsSuccess(driverStandings));
                    }else{
                        Log.e(TAG, "Junior driver standings not found");
                        fetchFromLocalDrivers(cacheKey, series);
                    }
                }

                @Override
                public void onConstructorStandingsLoaded(JuniorConstructorStandings constructorStandings) {

                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "1 Error loading junior driver standings: " + e.getMessage());
                    localJuniorStandingsDataSource.deleteJuniorDriverStandings(series);
                    Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                            .postValue(new Result.Error("Junior driver standings not available yet"));
                }
            });
        }catch (Exception e){
            Log.e(TAG, "2 Error loading junior driver standings: " + e.getMessage());
            fetchFromLocalDrivers(cacheKey, series);
        }

    }

    private void fetchFromLocalDrivers(String cacheKey, String series) {
        Log.i(TAG, "Fetching junior driver standings from local database: " + cacheKey);
        localJuniorStandingsDataSource.getJuniorDriverStandings(series, new JuniorStandingsCallback() {

            @Override
            public void onDriverStandingsLoaded(JuniorDriverStandings driverStandings) {
                if (driverStandings != null) {
                    juniorStandingsCache.put(cacheKey, new MutableLiveData<>(
                            new Result.JuniorDriverStandingsSuccess(driverStandings)));
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                            .postValue(new Result.JuniorDriverStandingsSuccess(driverStandings));

                }else{
                    Log.e(TAG, "Junior driver standings not found in local database");
                }
            }

            @Override
            public void onConstructorStandingsLoaded(JuniorConstructorStandings constructorStandings) {

            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading junior driver standings from local database: " + e.getMessage());
                Objects.requireNonNull(juniorStandingsCache.get(cacheKey))
                        .postValue(new Result.Error("Error loading junior driver standings: " + e.getMessage()));

            }
        });
    }
}
