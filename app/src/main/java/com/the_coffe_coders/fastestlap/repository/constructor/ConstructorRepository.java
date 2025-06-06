package com.the_coffe_coders.fastestlap.repository.constructor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.repository.driver.DriverCallback;
import com.the_coffe_coders.fastestlap.source.constructor.FirebaseConstructorDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.JolpicaConstructorDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.LocalConstructorDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConstructorRepository {
    private static ConstructorRepository instance;
    private final Map<String, MutableLiveData<Result>> constructorCache;
    private final Map<String, Long> lastUpdateTimestamps;
    private final FirebaseConstructorDataSource firebaseConstructorDataSource;
    private final JolpicaConstructorDataSource jolpicaConstructorDataSource;

    private final LocalConstructorDataSource localConstructorDataSource;
    private final String TAG = "ConstructorRepository";
    private Context context;

    private ConstructorRepository(AppRoomDatabase appRoomDatabase, Context context) {
        constructorCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        firebaseConstructorDataSource = FirebaseConstructorDataSource.getInstance();
        jolpicaConstructorDataSource = JolpicaConstructorDataSource.getInstance();
        localConstructorDataSource = LocalConstructorDataSource.getInstance(appRoomDatabase);
        this.context = context;
    }

    public static ConstructorRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new ConstructorRepository(appRoomDatabase, context);
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

    public synchronized MutableLiveData<Result> getConstructor(String constructorId) {
        if (!constructorCache.containsKey(constructorId) || !lastUpdateTimestamps.containsKey(constructorId) || lastUpdateTimestamps.get(constructorId) == null) {
            constructorCache.put(constructorId, new MutableLiveData<>());
            if(isNetworkAvailable()) {
                loadConstructor(constructorId);
            } else {
                Log.i(TAG, "No network connection");
                loadConstructorFromLocal(constructorId);
            }
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(constructorId) > 60000) {
            if(isNetworkAvailable()) {
                loadConstructor(constructorId);
            } else {
                Log.d(TAG, "No network connection");
                loadConstructorFromLocal(constructorId);
            }
        } else {
            Log.i(TAG, "Constructor found in cache: " + constructorId);
        }
        return constructorCache.get(constructorId);
    }

    private void loadConstructor(String constructorId) {
        constructorCache.get(constructorId).postValue(new Result.Loading("Fetching constructor from remote"));
        try {
            firebaseConstructorDataSource.getConstructor(constructorId, new ConstructorCallback() {
                @Override
                public void onConstructorLoaded(Constructor constructor) {
                    if (constructor != null) {
                        constructor.setConstructorId(constructorId);
                        localConstructorDataSource.insertConstructor(constructor);
                        lastUpdateTimestamps.put(constructorId, System.currentTimeMillis());
                        Objects.requireNonNull(constructorCache.get(constructorId)).postValue(new Result.ConstructorSuccess(constructor));
                    } else {
                        Log.e(TAG, "Constructor not found: " + constructorId);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading constructor: " + e.getMessage());
                    loadConstructorFromLocal(constructorId);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading constructor: " + e.getMessage());
            loadConstructorFromLocal(constructorId);
        }
    }

    private void loadConstructorFromLocal(String constructorId) {
        localConstructorDataSource.getConstructor(constructorId, new ConstructorCallback() {
            @Override
            public void onConstructorLoaded(Constructor constructor) {
                if (constructor != null) {
                    constructor.setConstructorId(constructorId);
                    constructorCache.put(constructorId, new MutableLiveData<>(new Result.ConstructorSuccess(constructor)));
                    lastUpdateTimestamps.put(constructorId, System.currentTimeMillis());
                    Objects.requireNonNull(constructorCache.get(constructorId)).postValue(new Result.ConstructorSuccess(constructor));
                } else {
                    Log.e(TAG, "Driver not found: " + constructorId);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading driver from local database: " + e.getMessage());
            }
        });
    }
}
