package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.repository.driver.DriverRepository;

public class DriverViewModel extends ViewModel {
    private final MutableLiveData<Driver> selectedDriverLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final DriverRepository driverRepository;

    public DriverViewModel(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public MutableLiveData<Result> getDriver(String driverId) {
        return driverRepository.getDriver(driverId);
    }
}
