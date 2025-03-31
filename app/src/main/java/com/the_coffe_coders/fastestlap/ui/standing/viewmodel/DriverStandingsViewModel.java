package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.standings.driver.DriverStandingRepository;
import com.the_coffe_coders.fastestlap.repository.standings.driver.DriverStandingsRepository;
import com.the_coffe_coders.fastestlap.source.driver_standings.DriverStandingsRemoteDataSource;

import java.util.List;

public class DriverStandingsViewModel extends ViewModel {
    private static final String TAG = DriverStandingsViewModel.class.getSimpleName();
    DriverStandingRepository driverStandingRepository;
    public DriverStandingsViewModel(DriverStandingRepository driverStandingRepository) {
        this.driverStandingRepository = driverStandingRepository;
    }

    public MutableLiveData<Result> getDriverStandingsLiveData() {
        return driverStandingRepository.fetchDriverStanding();
    }
}
