package com.the_coffe_coders.fastestlap.repository.nation;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.source.nation.FirebaseNationDataSource;
import com.the_coffe_coders.fastestlap.source.nation.LocalNationDataSource;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class NationRepository {
    private static final String TAG = "NationRepository";
    public static NationRepository instance;
    //Data sources
    final FirebaseNationDataSource firebaseNationDataSource;
    final LocalNationDataSource localNationDataSource;
    //Cache
    private final Map<String, MutableLiveData<Result>> nationCache;
    private final Map<String, Long> lastUpdateTimestamps;
    // Tracks which nation IDs are currently being fetched from Firebase.
    // Prevents duplicate Firebase reads when two callers request the same
    // nation ID before the first callback has completed and set the timestamp.
    private final Set<String> inFlightFetches;
    private final NetworkUtils networkLiveData;


    private NationRepository(AppRoomDatabase appRoomDatabase, Context context) {
        nationCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        inFlightFetches = new HashSet<>();
        firebaseNationDataSource = FirebaseNationDataSource.getInstance();
        localNationDataSource = LocalNationDataSource.getInstance(appRoomDatabase);
        networkLiveData = new NetworkUtils(context);
    }

    public static NationRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new NationRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    }

    public synchronized MutableLiveData<Result> getNation(String nationId) throws RuntimeException {
        Log.d(TAG, "Fetching nation with ID: " + nationId);

        if (!nationCache.containsKey(nationId)) {
            nationCache.put(nationId, new MutableLiveData<>());
        }

        if (!lastUpdateTimestamps.containsKey(nationId) || lastUpdateTimestamps.get(nationId) == null) {
            // No result yet. Only start a fetch if this nationId isn't already in-flight.
            if (!inFlightFetches.contains(nationId)) {
                inFlightFetches.add(nationId);
                if (isNetworkAvailable()) {
                    loadNation(nationId);
                } else {
                    loadNationFromLocal(nationId);
                }
            } else {
                Log.d(TAG, "Nation fetch already in-flight for: " + nationId);
            }
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(nationId) > 60000) {
            if (!inFlightFetches.contains(nationId)) {
                inFlightFetches.add(nationId);
                if (isNetworkAvailable()) {
                    loadNation(nationId);
                } else {
                    loadNationFromLocal(nationId);
                }
            }
        } else {
            Log.d(TAG, "Nation found in cache: " + nationId);
        }
        return nationCache.get(nationId);
    }

    public void loadNationFromLocal(String nationId) throws RuntimeException {
        localNationDataSource.getNation(nationId, new NationCallback() {
            @Override
            public void onNationLoaded(Nation nation) {
                if (nation != null) {
                    nation.setNationId(nationId);
                    localNationDataSource.insertNation(nation);
                    nationCache.put(nationId, new MutableLiveData<>(new Result.NationSuccess(nation)));
                    lastUpdateTimestamps.put(nationId, System.currentTimeMillis());
                    Objects.requireNonNull(nationCache.get(nationId)).postValue(new Result.NationSuccess(nation));
                } else {
                    Log.e(TAG, "Nation not found: " + nationId);
                    throw new RuntimeException("Nation not found in local database: " + nationId);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading nation from local database: " + e.getMessage());
            }
        });
    }

    private void loadNation(String nationId) {
        nationCache.get(nationId).postValue(new Result.Loading("Fetching nation from remote"));
        try {
            firebaseNationDataSource.getNation(nationId, new NationCallback() {
                @Override
                public void onNationLoaded(Nation nation) {
                    inFlightFetches.remove(nationId);
                    if (nation != null) {
                        nation.setNationId(nationId);
                        localNationDataSource.insertNation(nation);
                        lastUpdateTimestamps.put(nationId, System.currentTimeMillis());
                        Objects.requireNonNull(nationCache.get(nationId)).postValue(new Result.NationSuccess(nation));
                    } else {
                        Log.e(TAG, "Nation not found: " + nationId);
                    }
                }

                @Override
                public void onError(Exception e) {
                    inFlightFetches.remove(nationId);
                    Log.e(TAG, "Error loading nation: " + e.getMessage());
                    //fetch nation from local database
                    loadNationFromLocal(nationId);
                }
            });
        } catch (Exception e) {
            inFlightFetches.remove(nationId);
            Log.e(TAG, "Error loading nation: " + e.getMessage());
            loadNationFromLocal(nationId);
        }
    }
}