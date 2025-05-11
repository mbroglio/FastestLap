package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingRepository;

public class DriverStandingsViewModel extends ViewModel {
    private final MutableLiveData<DriverStandingsElement> selectedDriverLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final DriverStandingRepository driverStandingRepository;
    public DriverStandingsViewModel(DriverStandingRepository driverStandingRepository) {
        this.driverStandingRepository = driverStandingRepository;
    }

    public MutableLiveData<Result> getDriverStandingsLiveData() {
        return driverStandingRepository.getDriverStandings();
    }
}
