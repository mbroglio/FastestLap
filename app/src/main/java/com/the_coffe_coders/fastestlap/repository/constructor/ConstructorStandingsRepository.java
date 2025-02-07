package com.the_coffe_coders.fastestlap.repository.constructor;

import android.util.Log;

import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.mapper.ConstructorStandingsMapper;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorLocalDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorRemoteDataSource;

public class ConstructorStandingsRepository implements IConstructorRepository, ConstructorResponseCallback {

    public static long FRESH_TIMEOUT = 60000;
    public static boolean isOutdate = true;
    public final MediatorLiveData<Result> allConstructorMediatorLiveData;
    public final MutableLiveData<Result> jolpicaConstructorsMutableLiveData;
    private final MutableLiveData<Result> constructorStandingsMutableLiveData;
    private final BaseConstructorRemoteDataSource constructorRemoteDataSource;
    private final BaseConstructorLocalDataSource constructorLocalDataSource;
    String TAG = "ConstructorStandingsRepository";

    public ConstructorStandingsRepository(BaseConstructorRemoteDataSource constructorRemoteDataSource, BaseConstructorLocalDataSource constructorLocalDataSource) {
        this.allConstructorMediatorLiveData = new MediatorLiveData<>();
        this.jolpicaConstructorsMutableLiveData = new MutableLiveData<>();
        this.constructorStandingsMutableLiveData = new MutableLiveData<>();
        this.constructorRemoteDataSource = constructorRemoteDataSource;
        this.constructorLocalDataSource = constructorLocalDataSource;
        this.constructorRemoteDataSource.setConstructorCallback(this);
        this.constructorLocalDataSource.setConstructorCallback(this);
    }

    @Override
    public MutableLiveData<Result> fetchConstructorStandings(long lastUpdate) {
        long currentTime = System.currentTimeMillis();
        if (true) {
            Log.i(TAG, "FETCH CONSTRUCTORSTANDINGS METHOD");
            constructorRemoteDataSource.getConstructorStandings();
            isOutdate = false;
        } else {
            constructorLocalDataSource.getConstructorStandings();
        }
        return constructorStandingsMutableLiveData;
    }

    @Override
    public void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorStandingsAPIResponse, long lastUpdate) {
        if (constructorStandingsAPIResponse.getStandingsTable().getStandingsLists().isEmpty()) {
            //post an error on all...
            Log.i("onSuccessFromRemoteConstructor", "CONSTRUCTOR API RESPONSE EMPTY");
            constructorStandingsMutableLiveData.postValue(new Result.Error("No data available"));
            return;
        }
        Log.i("onSuccessFromRemoteConstructor", "CONSTRUCTOR API RESPONSE: " + constructorStandingsAPIResponse);
        ConstructorStandings constructorStandings = ConstructorStandingsMapper.toConstructorStandings(constructorStandingsAPIResponse.getStandingsTable().getStandingsLists().get(0));
        Log.i("onSuccessFromRemoteConstructor", "CONSTRUCTOR STANDINGS: " + constructorStandings);
        constructorLocalDataSource.insertConstructorsStandings(constructorStandings);
        isOutdate = false;
    }

    @Override
    public void onFailureFromRemote(Exception e) {

    }

    @Override
    public void onSuccessFromLocal(ConstructorStandings constructorStandings) {
        Log.i("onSuccessFromLocalConstructor", "CONSTRUCTOR STANDINGS: " + constructorStandings);
        Result.ConstructorStandingsSuccess result = new Result.ConstructorStandingsSuccess(constructorStandings);
        if (constructorStandings != null) {
            isOutdate = false;
            constructorStandingsMutableLiveData.postValue(result);
        } else {
            isOutdate = true;
            constructorRemoteDataSource.getConstructorStandings();
        }
    }

    @Override
    public void onFailureFromLocal(Exception e) {

    }
}
