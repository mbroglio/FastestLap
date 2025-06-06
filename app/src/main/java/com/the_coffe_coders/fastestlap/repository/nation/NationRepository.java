package com.the_coffe_coders.fastestlap.repository.nation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.source.nation.FirebaseNationDataSource;
import com.the_coffe_coders.fastestlap.source.nation.LocalNationDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NationRepository {
    private static final String TAG = "NationRepository";
    public static NationRepository instance;
    //Cache
    private final Map<String, MutableLiveData<Result>> nationCache;
    private final Map<String, Long> lastUpdateTimestamps;
    //Data sources
    FirebaseNationDataSource firebaseNationDataSource;
    LocalNationDataSource localNationDataSource;
    AppRoomDatabase appRoomDatabase;

    private Context context;

    private NationRepository(AppRoomDatabase appRoomDatabase, Context context) {
        nationCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        firebaseNationDataSource = FirebaseNationDataSource.getInstance();
        localNationDataSource = LocalNationDataSource.getInstance(appRoomDatabase);
        this.context = context;
    }

    public static NationRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new NationRepository(appRoomDatabase, context);
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


    public synchronized MutableLiveData<Result> getNation(String nationId) throws RuntimeException{
        Log.d(TAG, "Fetching nation with ID: " + nationId);
        if (!nationCache.containsKey(nationId) || !lastUpdateTimestamps.containsKey(nationId) || lastUpdateTimestamps.get(nationId) == null) {
            nationCache.put(nationId, new MutableLiveData<>());

            if(isNetworkAvailable()) {
                loadNation(nationId);
            }else {
                loadNationFromLocal(nationId);
            }
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(nationId) > 6000) {
            if(isNetworkAvailable()) {
                loadNation(nationId);
            }else {
                loadNationFromLocal(nationId);
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
                    Log.e(TAG, "Error loading nation: " + e.getMessage());
                    //fetch nation from local database
                    loadNationFromLocal(nationId);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading nation: " + e.getMessage());
            loadNationFromLocal(nationId);
        }
    }
}