package com.the_coffe_coders.fastestlap.repository.driver;


import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.driver.DriverAPIResponse;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.mapper.DriverStandingsMapper;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverRemoteDataSource;

import java.util.List;

public class DriverRepository implements IDriverRepository, DriverResponseCallback {

    String TAG = "DriverRepository";

    public static long FRESH_TIMEOUT = 60000;

    private final MutableLiveData<Result> allDriverMutableLiveData;//shuld be final
    private final MutableLiveData<Result> driverStandingsMutableLiveData;//shuld be final
    private final BaseDriverRemoteDataSource driverRemoteDataSource;//shuld be final
    private final BaseDriverLocalDataSource driverLocalDataSource;//shuld be final

    public DriverRepository(BaseDriverRemoteDataSource driverRemoteDataSource, BaseDriverLocalDataSource driverLocalDataSource) {
        this.allDriverMutableLiveData = new MutableLiveData<>();
        this.driverStandingsMutableLiveData = new MutableLiveData<>();
        this.driverRemoteDataSource = driverRemoteDataSource;
        this.driverLocalDataSource = driverLocalDataSource;
        this.driverRemoteDataSource.setDriverCallback(this);
        this.driverLocalDataSource.setDriverCallback(this);
    }

    @Override
    public MutableLiveData<Result> fetchDrivers(long lastUpdate) {
        long currentTime = System.currentTimeMillis();
        System.out.println(currentTime);
        System.out.println(lastUpdate);
        System.out.println("FETCH DRIVER METHOD");
        Log.i(TAG, "FETCH DRIVER METHOD");
        if(currentTime - lastUpdate > FRESH_TIMEOUT) { //currentTime - lastUpdate > FRESH_TIMEOUT
            driverRemoteDataSource.getDrivers();
        } else {
          driverLocalDataSource.getDrivers();
        }

        return allDriverMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> fetchDriver(String driverId, long lastUpdate) {
        long currentTime = System.currentTimeMillis();

        if(true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT
            driverRemoteDataSource.getDrivers();
        } else {
            driverLocalDataSource.getDrivers();
        }

        return null;
    }
    @Override
    public MutableLiveData<Result> fetchDriversStandings(long lastUpdate) {
        long currentTime = System.currentTimeMillis();
        //controllo che il dispositivo sia collegato ad internet
        if(true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT
            Log.i(TAG, "FETCH DRIVER STANDINGS METHOD");
            driverRemoteDataSource.getDriversStandings();
        } else {
            driverLocalDataSource.getDriversStandings();
        }
        return driverStandingsMutableLiveData;
    }

    /*@Override
    public void onSuccessFromRemote(DriversAPIResponse driverAPIResponse, long lastUpdate) {

        List<Driver> drivers = new ArrayList<>();

        for (DriverStandingsElementDTO driver: driverAPIResponse.getStandingsTable().getStandingsLists().get(0).getDriverStandings()) {
            drivers.add(DriverMapper.toDriver(driver.getDriver()));
            Log.i("onSuccessFromRemoteDriver", "DRIVER: " + driver.getDriver());
        }
        driverLocalDataSource.insertDrivers(drivers);
    }*/

    @Override
    public void onSuccessFromRemote(DriverStandingsAPIResponse driverAPIResponse, long lastUpdate) {
        Log.i("onSuccessFromRemoteDriver", "DRIVER API RESPONSE: " + driverAPIResponse);
        DriverStandings driverStandings = DriverStandingsMapper.toDriverStandings(driverAPIResponse.getStandingsTable().getStandingsLists().get(0));
        Log.i("onSuccessFromRemoteDriver", "DRIVER STANDINGS: " + driverStandings);
        driverLocalDataSource.insertDriversStandings(driverStandings);
    }

    @Override
    public void onFailureFromRemote(Exception exception) {

    }

    @Override
    public void onSuccessFromLocal(List<Driver> driverList) {
        Result.DriverSuccess result = new Result.DriverSuccess(new DriverAPIResponse(driverList));
        allDriverMutableLiveData.postValue(result);
    }

    public void onSuccessFromLocal(DriverStandings driverStandings) {
        Log.i("onSuccessFromLocalDriver", "DRIVER STANDINGS: " + driverStandings);
        Result.DriverStandingsSuccess result = new Result.DriverStandingsSuccess(driverStandings);
        driverStandingsMutableLiveData.postValue(result);
    }

    @Override
    public void onFailureFromLocal(Exception exception) {

    }
}


