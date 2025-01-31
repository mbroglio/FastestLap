package com.the_coffe_coders.fastestlap.repository.constructor;


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.mapper.ConstructorStandingsMapper;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorLocalDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorRemoteDataSource;

import java.util.List;

public class ConstructorRepository implements IConstructorRepository, ConstructorResponseCallback {

    public static long FRESH_TIMEOUT = 60000;
    public static boolean isOutdate = true;
    private static ConstructorRepository instance;
    private final MutableLiveData<Result> constructorStandingsMutableLiveData;
    private final MutableLiveData<Result> constructorMutableLiveData;
    private final BaseConstructorRemoteDataSource constructorRemoteDataSource;
    private final BaseConstructorLocalDataSource constructorLocalDataSource;
    String TAG = "ConstructorRepository";

    public ConstructorRepository(BaseConstructorRemoteDataSource constructorRemoteDataSource, BaseConstructorLocalDataSource constructorLocalDataSource) {
        this.constructorStandingsMutableLiveData = new MutableLiveData<>();
        this.constructorMutableLiveData = new MutableLiveData<>();
        this.constructorRemoteDataSource = constructorRemoteDataSource;
        this.constructorLocalDataSource = constructorLocalDataSource;
        this.constructorRemoteDataSource.setConstructorCallback(this);
        this.constructorLocalDataSource.setConstructorCallback(this);
    }

    @Override
    public final List<Constructor> findConstructors() {
        return null;
    }

    @Override
    public Constructor find(String id) {
        return null;
    }

    public LiveData<Constructor> fetchConstructor(String constructorId) {
        Constructor constructor = null;
        for (Constructor current : findConstructors()) {
            if (current.getConstructorId().equals(constructorId))
                constructor = current;
        }
        return new LiveData<Constructor>() {
        };
    }

    public MutableLiveData<Result> fetchConstructorStandings(long lastUpdate) {
        long currentTime = System.currentTimeMillis();
        Log.i(TAG, "fetchConstructorStandings");

        if (true) { //currentTime - lastUpdate > FRESH_TIMEOUT
            constructorRemoteDataSource.getConstructor();
        } else {
            constructorLocalDataSource.getConstructor();
        }

        return constructorStandingsMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> fetchConstructors(long lastUpdate) {
        return null;
    }

    public MutableLiveData<Result> fetchConstructor(String constructorId, long lastUpdate) {
        long currentTime = System.currentTimeMillis();

        if (isOutdate) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT
            constructorRemoteDataSource.getConstructor();
        } else {
            constructorLocalDataSource.getConstructor();
        }

        return null;
    }


    @Override
    public void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorAPIResponse, long lastUpdate) {
        Log.i("onSuccessFromRemoteConstructor", "CONSTRUCTOR API RESPONSE: " + constructorAPIResponse);
        ConstructorStandings constructorsStandings = ConstructorStandingsMapper.toConstructorStandings(constructorAPIResponse.getStandingsTable().getStandingsLists().get(0));
        constructorLocalDataSource.insertConstructorsStandings(constructorsStandings);
    }

    @Override
    public void onFailureFromRemote(Exception e) {

    }

    @Override
    public void onSuccessFromLocal(ConstructorStandings constructorStandings) {
        Log.i("onSuccessFromLocalConstructor", "CONSTRUCTOR STANDINGS: " + constructorStandings);
        Result.ConstructorStandingsSuccess result = new Result.ConstructorStandingsSuccess(constructorStandings);
        constructorStandingsMutableLiveData.postValue(result);
        isOutdate = false;
    }

    @Override
    public void onFailureFromLocal(Exception e) {

    }
}