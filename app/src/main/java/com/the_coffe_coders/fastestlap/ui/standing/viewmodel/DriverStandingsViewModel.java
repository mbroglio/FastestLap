package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.driver.DriverRepository;

public class DriverStandingsViewModel extends ViewModel {
    private static final String TAG = DriverStandingsViewModel.class.getSimpleName();

    private final DriverRepository driverRepository;
    private final int page;
    private MutableLiveData<Result> driverStandingsLiveData;
    private MutableLiveData<Result> favoriteNewsListLiveData;

    public DriverStandingsViewModel(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
        this.page = 3;//TODO Verify
    }

    public MutableLiveData<Result> getDriverStandingsLiveData(long lastUpdate) {
        if(driverStandingsLiveData != null) {
            fetchDriverStandings(lastUpdate);
        }
        return driverStandingsLiveData;
    }

    private void fetchDriverStandings(long lastUpdate) {
        driverStandingsLiveData = driverRepository.fetchDriversStandings(lastUpdate);
    }
}
