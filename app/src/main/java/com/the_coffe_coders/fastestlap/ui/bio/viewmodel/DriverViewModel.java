package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.the_coffe_coders.fastestlap.repository.driver.DriverRepository;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriverViewModel extends ViewModel {

    private DriverRepository driverRepository;//TODO shuld be final

    /*DriverViewModel() {
        driverRepository = DriverRepository.getInstance();
    }*/
    private final MutableLiveData<Driver> selectedDriverLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);


    public DriverViewModel() {
        //driverRepository = DriverRepository.getInstance();
    }

    public LiveData<Driver> getSelectedDriverLiveData() {
        return selectedDriverLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public void fetchDriverById(String driverId) {
        isLoading.setValue(true);
        executorService.execute(() -> {
            try {
                Driver driver = driverRepository.find(driverId);
                selectedDriverLiveData.postValue(driver);
                isLoading.postValue(false);
            } catch (Exception e) {
                errorLiveData.postValue("Failed to fetch driver with ID: " + driverId + ". Error: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }



}
