package com.the_coffe_coders.fastestlap.repository.driver;


import android.util.Log;

import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.mapper.DriverStandingsMapper;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverRemoteDataSource;

import java.util.List;

public class DriverStandingsStandingsRepository implements IDriverStandingsRepository, DriverStandingsResponseCallback {
    private static final String TAG = "DriverStandingsRepository";

    public static long FRESH_TIMEOUT = 60000;
    public static boolean isOutdate = true;

    private final MediatorLiveData<Result> allDriverMediatorLiveData;
    private final MutableLiveData<Result> jolpicaDriversMutableLiveData;
    private final MutableLiveData<Result> driverStandingsMutableLiveData;
    private final BaseDriverRemoteDataSource driverRemoteDataSource;
    private final BaseDriverLocalDataSource driverLocalDataSource;

    //private Map<String, Driver> cache = new HashMap<>();//TODO

    public DriverStandingsStandingsRepository(BaseDriverRemoteDataSource driverRemoteDataSource, BaseDriverLocalDataSource driverLocalDataSource) {
        this.allDriverMediatorLiveData = new MediatorLiveData<>();
        this.jolpicaDriversMutableLiveData = new MutableLiveData<>();
        this.driverStandingsMutableLiveData = new MutableLiveData<>();
        this.driverRemoteDataSource = driverRemoteDataSource;
        this.driverLocalDataSource = driverLocalDataSource;
        this.driverRemoteDataSource.setDriverCallback(this);
        this.driverLocalDataSource.setDriverCallback(this);
    }

    @Override
    public MutableLiveData<Result> fetchDriversStandings(long lastUpdate) {
        long currentTime = System.currentTimeMillis();
        //controllo che il dispositivo sia collegato ad internet
        Log.i(TAG, "FETCH DRIVER STANDINGS METHOD");
        if (true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT
            Log.i(TAG, "FETCH DRIVER STANDINGS METHOD");
            driverRemoteDataSource.getDriversStandings();
            isOutdate = false;
        } else {
            Log.i(TAG, "" + isOutdate);
            driverLocalDataSource.getDriversStandings();
        }
        return driverStandingsMutableLiveData;
    }

    @Override
    public void onSuccessFromRemote(DriverStandingsAPIResponse driverAPIResponse, long lastUpdate) {
        Log.i("onSuccessFromRemoteDriver", "DRIVER API RESPONSE: " + driverAPIResponse);
        if(driverAPIResponse.getStandingsTable().getStandingsLists().isEmpty()) {
            //post an error on all...
            Log.i("onSuccessFromRemoteDriver", "DRIVER API RESPONSE EMPTY");
            driverStandingsMutableLiveData.postValue(new Result.Error("No data available"));
            return;
        }

        DriverStandings driverStandings = DriverStandingsMapper.toDriverStandings(driverAPIResponse.getStandingsTable().getStandingsLists().get(0));
        Log.i("onSuccessFromRemoteDriver", "DRIVER STANDINGS: " + driverStandings);
        driverLocalDataSource.insertDriversStandings(driverStandings);

    }

    @Override
    public void onFailureFromRemote(Exception exception) {

    }

    @Override
    public void onSuccessFromLocal(List<Driver> driverList) {
        //Result.DriverSuccess result = new Result.DriverSuccess(new DriverAPIResponse(driverList));
        //allDriverMutableLiveData.postValue(result);
    }

    public void onSuccessFromLocal(DriverStandings driverStandings) {
        Log.i("onSuccessFromLocalDriver", "DRIVER STANDINGS: " + driverStandings);
        Result.DriverStandingsSuccess result = new Result.DriverStandingsSuccess(driverStandings);
        driverStandingsMutableLiveData.postValue(result);
    }

    @Override
    public void onFailureFromLocal(Exception exception) {}
}