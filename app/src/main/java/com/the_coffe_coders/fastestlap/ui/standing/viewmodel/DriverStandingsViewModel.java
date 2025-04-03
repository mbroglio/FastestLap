package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingRepository;

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
