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
    private MutableLiveData<Result> driverStandingsLiveData;

    public DriverStandingsViewModel(DriverStandingsRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public MutableLiveData<Result> getDriverStandingsLiveData(long lastUpdate) {
        Log.i(TAG, "getDriverStandingsLiveData");
        if (driverStandingsLiveData == null) {
            fetchDriverStandings(lastUpdate);
        }
        return driverStandingsLiveData;
    }

    private void fetchDriverStandings(long lastUpdate) {
        Log.i(TAG, "fetchDriverStandings");
        this.driverStandingsLiveData = driverRepository.fetchDriversStandings(lastUpdate);
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
