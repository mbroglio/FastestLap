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
            loadNationCacheFirst(nationId);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(nationId);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) {
                if (isNetworkAvailable() && !inFlightFetches.contains(nationId)) {
                    inFlightFetches.add(nationId);
                    loadNationFromRemote(nationId, true);
                }
            } else {
                Log.d(TAG, "Nation found in cache: " + nationId);
            }
        }
        return nationCache.get(nationId);
    }

    private void loadNationCacheFirst(String nationId) {
        localNationDataSource.getNation(nationId, new NationCallback() {
            @Override
            public void onNationLoaded(Nation nation) {
                if (nation != null) {
                    Log.d(TAG, "Nation loaded from local database (cache hit): " + nationId);
                    nation.setNationId(nationId);
                    lastUpdateTimestamps.put(nationId, System.currentTimeMillis());
                    Objects.requireNonNull(nationCache.get(nationId)).postValue(new Result.NationSuccess(nation));

                    // Only refresh from remote if the cached data is actually stale.
                    // Without this TTL guard, Firebase fires on every launch even when the
                    // local data is fresh, causing the LiveData to re-emit and triggering
                    // redundant card rebuilds in the UI.
                    Long ts = lastUpdateTimestamps.get(nationId);
                    boolean isStale = ts == null || System.currentTimeMillis() - ts > 300_000L;
                    if (isNetworkAvailable() && isStale && !inFlightFetches.contains(nationId)) {
                        inFlightFetches.add(nationId);
                        loadNationFromRemote(nationId, true);
                    } else {
                        Log.d(TAG, "Nation cache still fresh, skipping remote refresh: " + nationId);
                    }
                } else {
                    Log.d(TAG, "Nation cache miss in local database: " + nationId);
                    if (isNetworkAvailable() && !inFlightFetches.contains(nationId)) {
                        inFlightFetches.add(nationId);
                        loadNationFromRemote(nationId, false);
                    } else if (!inFlightFetches.contains(nationId)) {
                        Objects.requireNonNull(nationCache.get(nationId)).postValue(
                                new Result.Error("Nation not found locally and no network connection available"));
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking local database for nation: " + e.getMessage());
                if (isNetworkAvailable() && !inFlightFetches.contains(nationId)) {
                    inFlightFetches.add(nationId);
                    loadNationFromRemote(nationId, false);
                }
            }
        });
    }

    private void loadNationFromRemote(String nationId, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            nationCache.get(nationId).postValue(new Result.Loading("Fetching nation from remote"));
        }
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
                    } else if (!isBackgroundRefresh) {
                        Log.e(TAG, "Nation not found: " + nationId);
                    }
                }

                @Override
                public void onError(Exception e) {
                    inFlightFetches.remove(nationId);
                    Log.e(TAG, "Error loading nation from remote: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            inFlightFetches.remove(nationId);
            Log.e(TAG, "Error loading nation from remote: " + e.getMessage());
        }
    }
}