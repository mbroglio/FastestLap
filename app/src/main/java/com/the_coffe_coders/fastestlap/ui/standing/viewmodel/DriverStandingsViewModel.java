package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.driver.DriverStandingsRepository;

import java.util.List;

public class DriverStandingsViewModel extends ViewModel {
    private static final String TAG = DriverStandingsViewModel.class.getSimpleName();
    private final DriverStandingsRepository driverRepository;
    private final MutableLiveData<Result> driverStandings;
    private final MutableLiveData<Boolean> isLoadingLiveData;
    private final MutableLiveData<String> errorMessageLiveData;
    private long lastUpdateTimestamp;

    public DriverStandingsViewModel(DriverStandingsRepository driverRepository) {
        this.driverRepository = driverRepository;
        driverStandings = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>();
        errorMessageLiveData = new MutableLiveData<>();
    }

    public MutableLiveData<Result> getDriverStandingsLiveData(long lastUpdate) {
        Log.i(TAG, "getDriverStandingsLiveData");
        fetchDriverStandings(lastUpdate);
        return driverStandings;
    }

    private void fetchDriverStandings(long lastUpdate) {
        //isLoadingLiveData.setValue(true);
        errorMessageLiveData.setValue(null);
        driverRepository.fetchDriversStandings(lastUpdateTimestamp)
                .thenAccept(result -> {
                    isLoadingLiveData.postValue(false);
                    if (result instanceof Result.Error) {
                        //errorMessageLiveData.postValue(((Result.Error) result).getError());
                    } else if (result instanceof Result.DriverStandingsSuccess) {
                        lastUpdateTimestamp = System.currentTimeMillis();
                    }
                    driverStandings.postValue(result);
                })
                .exceptionally(throwable -> {
                    //isLoadingLiveData.postValue(false);
                    errorMessageLiveData.postValue("Si è verificato un errore: " + throwable.getMessage());
                    return null;
                });
    }

    public DriverStandingsElement getDriverStandingsElement(List<DriverStandingsElement> driversList, String driverID) {
        for (DriverStandingsElement driver : driversList) {
            if (driver.getDriver().getDriverId().equals(driverID)) {
                return driver;
            }
        }
        return null;
    }
}
