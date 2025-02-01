package com.the_coffe_coders.fastestlap.repository.constructor;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.mapper.ConstructorStandingsMapper;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorLocalDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorRemoteDataSource;

import java.util.Collections;
import java.util.List;

public class ConstructorStandingsRepository implements IConstructorRepository, ConstructorResponseCallback {

    String TAG = "ConstructorStandingsRepository";
    public static long FRESH_TIMEOUT = 60000;
    public static boolean isOutdate = true;
    public final MediatorLiveData<Result> allConstructorMediatorLiveData;
    public final MutableLiveData<Result> jolpicaConstructorsMutableLiveData;
    private final MutableLiveData<Result> allConstructorMutableLiveData;
    private final MutableLiveData<Result> constructorStandingsMutableLiveData;
    private final BaseConstructorRemoteDataSource constructorRemoteDataSource;
    private final BaseConstructorLocalDataSource constructorLocalDataSource;

    public ConstructorStandingsRepository(BaseConstructorRemoteDataSource constructorRemoteDataSource, BaseConstructorLocalDataSource constructorLocalDataSource) {
        this.allConstructorMediatorLiveData = new MediatorLiveData<>();
        this.jolpicaConstructorsMutableLiveData = new MutableLiveData<>();
        this.allConstructorMutableLiveData = new MutableLiveData<>();
        this.constructorStandingsMutableLiveData = new MutableLiveData<>();
        this.constructorRemoteDataSource = constructorRemoteDataSource;
        this.constructorLocalDataSource = constructorLocalDataSource;
        this.constructorRemoteDataSource.setConstructorCallback(this);
        this.constructorLocalDataSource.setConstructorCallback(this);
    }

    public LiveData<Result> getConstructors() {
        return allConstructorMediatorLiveData;
    }

    @Override
    public MutableLiveData<Result> fetchConstructors(long lastUpdate) {
        long currentTime = System.currentTimeMillis();
        System.out.println(currentTime);
        System.out.println(lastUpdate);
        System.out.println("FETCH CONSTRUCTOR METHOD");
        Log.i(TAG, "FETCH CONSTRUCTOR METHOD");
        if (currentTime - lastUpdate > FRESH_TIMEOUT) { //currentTime - lastUpdate > FRESH_TIMEOUT
            constructorRemoteDataSource.getConstructor();
        } else {
            constructorLocalDataSource.getConstructor();
        }
        return allConstructorMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> fetchConstructor(String constructorId, long lastUpdate) {
        long currentTime = System.currentTimeMillis();

        if (isOutdate) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT
            constructorRemoteDataSource.getConstructor();
            isOutdate = false;
        } else {
            constructorLocalDataSource.getConstructor();
        }
        return null;
    }

    @Override
    public MutableLiveData<Result> fetchConstructorStandings(long lastUpdate) {
        long currentTime = System.currentTimeMillis();
        Log.i(TAG, "FETCH CONSTRUCTORSTANDINGS METHOD");
        if (isOutdate) {
            Log.i(TAG, "FETCH CONSTRUCTORSTANDINGS METHOD");
            constructorRemoteDataSource.getConstructorStandings();
            isOutdate = false;
        } else {
            Log.i(TAG, "" + isOutdate);
            constructorLocalDataSource.getConstructorStandings();
        }
        return constructorStandingsMutableLiveData;
    }

    @Override
    public void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorStandingsAPIResponse, long lastUpdate) {
        Log.i("onSuccessFromRemoteConstructor", "CONSTRUCTOR API RESPONSE: " + constructorStandingsAPIResponse);
        ConstructorStandings constructorStandings = ConstructorStandingsMapper.toConstructorStandings(constructorStandingsAPIResponse.getStandingsTable().getStandingsLists().get(0));
        Log.i("onSuccessFromRemoteConstructor", "CONSTRUCTOR STANDINGS: " + constructorStandings);
        constructorLocalDataSource.insertConstructorsStandings(constructorStandings);
    }

    @Override
    public void onFailureFromRemote(Exception e) {

    }

    @Override
    public void onSuccessFromLocal(ConstructorStandings constructorStandings) {
        Log.i("onSuccessFromLocalConstructor", "CONSTRUCTOR STANDINGS: " + constructorStandings);
        Result.ConstructorStandingsSuccess result = new Result.ConstructorStandingsSuccess(constructorStandings);
        constructorStandingsMutableLiveData.postValue(result);
    }

    @Override
    public void onFailureFromLocal(Exception e) {

    }

    @Override
    public List<Constructor> findConstructors() {
        return Collections.emptyList();
    }

    @Override
    public Constructor find(String id) {
        return null;
    }




}
