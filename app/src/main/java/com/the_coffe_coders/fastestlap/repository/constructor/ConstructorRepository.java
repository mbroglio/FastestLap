package com.the_coffe_coders.fastestlap.repository.constructor;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.source.constructor.FirebaseConstructorDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.JolpicaConstructorDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConstructorRepository {
    private static ConstructorRepository instance;
    private final Map<String, MutableLiveData<Result>> constructorCache;
    private final Map<String, Long> lastUpdateTimestamps;
    private final FirebaseConstructorDataSource firebaseConstructorDataSource;
    private final JolpicaConstructorDataSource jolpicaConstructorDataSource;
    private final String TAG = "ConstructorRepository";

    private ConstructorRepository() {
        constructorCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        firebaseConstructorDataSource = FirebaseConstructorDataSource.getInstance();
        jolpicaConstructorDataSource = JolpicaConstructorDataSource.getInstance();
    }

    public static ConstructorRepository getInstance() {
        if (instance == null) {
            instance = new ConstructorRepository();
        }
        return instance;
    }

    public synchronized MutableLiveData<Result> getConstructor(String constructorId) {
        if (!constructorCache.containsKey(constructorId) || !lastUpdateTimestamps.containsKey(constructorId) || lastUpdateTimestamps.get(constructorId) == null) {
            constructorCache.put(constructorId, new MutableLiveData<>());
            loadConstructor(constructorId);
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(constructorId) > 60000) {
            loadConstructor(constructorId);
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
                        lastUpdateTimestamps.put(constructorId, System.currentTimeMillis());
                        Objects.requireNonNull(constructorCache.get(constructorId)).postValue(new Result.ConstructorSuccess(constructor));
                    } else {
                        Log.e(TAG, "Constructor not found: " + constructorId);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading constructor: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading constructor: " + e.getMessage());
        }
    }
}
