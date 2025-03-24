package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.repository.driver.CommonDriverRepository;
import com.the_coffe_coders.fastestlap.repository.standings.DriverStandingsRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriverViewModel extends ViewModel {

    private final CommonDriverRepository driverRepository;

    /*DriverViewModel() {
        driverRepository = DriverRepository.getInstance();
    }*/
    private final MutableLiveData<Driver> selectedDriverLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private DriverStandingsRepository driverStandingsRepository;//TODO shuld be final


    public DriverViewModel(CommonDriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public MutableLiveData<Result> getDriver(String driverId) {
        MutableLiveData<Result> driverLiveData = new MutableLiveData<>();

        // Show loading state
        //driverLiveData.setValue(new Result.Loading());

        // Get the driver using the CompletableFuture-based repository
        driverRepository.getDriver(driverId)
                .thenAccept(result -> {
                    // Update the LiveData on the main thread
                    runOnMainThread(() -> driverLiveData.setValue(result));
                })
                .exceptionally(throwable -> {
                    // Handle any exceptions that weren't caught in the repository
                    runOnMainThread(() -> driverLiveData.setValue(
                            new Result.Error("Error fetching driver: " + throwable.getMessage())
                    ));
                    return null;
                });

        return driverLiveData;
    }

    // Helper method to run code on the main thread
    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
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

}
