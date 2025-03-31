package com.the_coffe_coders.fastestlap.ui.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.standing.constructor.ConstructorStandingsStandingsRepository;
import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;
import com.the_coffe_coders.fastestlap.source.standing.driver.DriverStandingsRemoteDataSource;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private static final String TAG = EventViewModel.class.getSimpleName();

    // Driver related fields
    private final MutableLiveData<Result> driverStandings;
    // Constructor related fields
    private final ConstructorStandingsStandingsRepository constructorRepository;
    private final MutableLiveData<Result> constructorStandings;
    // Race related fields
    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;
    // Common UI state fields
    private final MutableLiveData<String> errorMessageLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;
    private long driverLastUpdateTimestamp = 0;
    private long constructorLastUpdateTimestamp = 0;
    private MutableLiveData<Result> nextRace;

    public HomeViewModel(RaceRepository raceRepository,
                         RaceResultRepository raceResultRepository,

                         ConstructorStandingsStandingsRepository constructorRepository) {
        this.raceRepository = raceRepository;
        this.raceResultRepository = raceResultRepository;
        this.constructorRepository = constructorRepository;

        // Initialize LiveData objects
        driverStandings = new MutableLiveData<>();
        constructorStandings = new MutableLiveData<>();
        errorMessageLiveData = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>();
    }

    public MutableLiveData<Result> getLastRace(long lastUpdate) {
        return raceRepository.fetchLastRace(lastUpdate);
    }

    public LiveData<Result> getDriverStandings() {
        return driverStandings;
    }

    public LiveData<Result> getConstructorStandings() {
        return constructorStandings;
    }

    public LiveData<Boolean> isLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    // Method to fetch constructor standings data with similar approach to drivers
    private void fetchConstructorStandings(long lastUpdate) {
        isLoadingLiveData.setValue(true);
        errorMessageLiveData.setValue(null);
        constructorRepository.fetchConstructorStandings(lastUpdate)
                .thenAccept(result -> {
                    isLoadingLiveData.postValue(false);
                    if (result instanceof Result.Error) {
                        errorMessageLiveData.postValue(result.getError());
                    } else if (result instanceof Result.ConstructorStandingsSuccess) {
                        constructorLastUpdateTimestamp = System.currentTimeMillis();
                    }
                    constructorStandings.postValue(result);
                })
                .exceptionally(throwable -> {
                    isLoadingLiveData.postValue(false);
                    errorMessageLiveData.postValue("An error occurred: " + throwable.getMessage());
                    return null;
                });
    }

    private void fetchNextRace(long lastUpdate) {
        this.nextRace = raceRepository.fetchNextRace(lastUpdate);
    }

    public MutableLiveData<Result> getDriverStandingsLiveData() {
        return DriverStandingRepository.getInstance(new DriverStandingsRemoteDataSource()).fetchDriverStanding();
    }

    public MutableLiveData<Result> getConstructorStandingsLiveData(long lastUpdate) {
        fetchConstructorStandings(lastUpdate);
        return constructorStandings;
    }

    public MutableLiveData<Result> getNextRaceLiveData(long lastUpdate) {
        if (nextRace == null) {
            fetchNextRace(lastUpdate);
        }
        return nextRace;
    }

    // Helper method to find a specific driver in the standings list
    public DriverStandingsElement getDriverStandingsElement(List<DriverStandingsElement> driversList, String driverId) {
        for (DriverStandingsElement driver : driversList) {
            if (driver.getDriver().getDriverId().equals(driverId)) {
                return driver;
            }
        }
        return null;
    }

    // Helper method to find a specific constructor in the standings list
    public ConstructorStandingsElement getConstructorStandingsElement(List<ConstructorStandingsElement> constructorList, String constructorId) {
        for (ConstructorStandingsElement constructor : constructorList) {
            if (constructor.getConstructor().getConstructorId().equals(constructorId)) {
                return constructor;
            }
        }
        return null;
    }
}