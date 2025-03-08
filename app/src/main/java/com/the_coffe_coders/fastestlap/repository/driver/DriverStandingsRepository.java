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
import java.util.concurrent.CompletableFuture;

public class DriverStandingsRepository implements IDriverStandingsRepository, DriverStandingsResponseCallback {
    private static final String TAG = "DriverStandingsRepository";
    public static long FRESH_TIMEOUT = 60000;
    private final BaseDriverRemoteDataSource driverRemoteDataSource;
    private final BaseDriverLocalDataSource driverLocalDataSource;
    // CompletableFuture per gestire le operazioni asincrone e restituire i risultati
    private CompletableFuture<Result> currentDriverStandingsFuture;
    public DriverStandingsRepository(BaseDriverRemoteDataSource driverRemoteDataSource, BaseDriverLocalDataSource driverLocalDataSource) {
        this.driverRemoteDataSource = driverRemoteDataSource;
        this.driverLocalDataSource = driverLocalDataSource;
        this.driverRemoteDataSource.setDriverCallback(this);
        this.driverLocalDataSource.setDriverCallback(this);
    }

    @Override
    public CompletableFuture<Result> fetchDriversStandings(long lastUpdate) {
        long currentTime = System.currentTimeMillis();
        currentDriverStandingsFuture = new CompletableFuture<>();
        if (currentTime - lastUpdate > FRESH_TIMEOUT) {
            Log.i(TAG, "FETCHING FROM REMOTE SOURCE");
            driverRemoteDataSource.getDriversStandings();
        } else {
            Log.i(TAG, "FETCHING FROM LOCAL SOURCE");
            driverLocalDataSource.getDriversStandings();
        }

        return currentDriverStandingsFuture;
    }

    @Override
    public void onSuccessFromRemote(DriverStandingsAPIResponse driverAPIResponse, long lastUpdate) {
        Log.i("onSuccessFromRemoteDriver", "DRIVER API RESPONSE: " + driverAPIResponse);
        if(driverAPIResponse.getStandingsTable().getStandingsLists().isEmpty()) {
            Log.i("onSuccessFromRemoteDriver", "DRIVER API RESPONSE EMPTY");
            currentDriverStandingsFuture.complete(new Result.Error("No data available"));
            return;
        }

        DriverStandings driverStandings = DriverStandingsMapper.toDriverStandings(
                driverAPIResponse.getStandingsTable().getStandingsLists().get(0));

        Log.i("onSuccessFromRemoteDriver", "DRIVER STANDINGS: " + driverStandings);
        driverLocalDataSource.insertDriversStandings(driverStandings);

        // Non completare ancora il future, aspetta che l'inserimento locale sia completato
    }

    @Override
    public void onFailureFromRemote(Exception exception) {
        Log.e(TAG, "Remote data fetch failed", exception);
        // Prova a recuperare i dati dal database locale come fallback
        driverLocalDataSource.getDriversStandings();
    }

    @Override
    public void onSuccessFromLocal(List<Driver> driverList) {
        // Questo metodo non è utilizzato per gli standings, ma è richiesto dall'interfaccia
    }

    public void onSuccessFromLocal(DriverStandings driverStandings) {
        Log.i("onSuccessFromLocalDriver", "DRIVER STANDINGS: " + driverStandings);
        Result.DriverStandingsSuccess result = new Result.DriverStandingsSuccess(driverStandings);
        if (currentDriverStandingsFuture != null && !currentDriverStandingsFuture.isDone()) {
            currentDriverStandingsFuture.complete(result);
        }
    }

    @Override
    public void onFailureFromLocal(Exception exception) {
        Log.e(TAG, "Local data fetch failed", exception);
        if (currentDriverStandingsFuture != null && !currentDriverStandingsFuture.isDone()) {
            currentDriverStandingsFuture.complete(new Result.Error("Failed to get driver standings: " + exception.getMessage()));
        }
    }
}