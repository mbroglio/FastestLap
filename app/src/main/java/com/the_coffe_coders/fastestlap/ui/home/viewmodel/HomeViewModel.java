package com.the_coffe_coders.fastestlap.ui.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.standing.constructor.ConstructorStandingRepository;
import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.WeeklyRaceRepository;
import com.the_coffe_coders.fastestlap.source.standing.driver.DriverStandingsRemoteDataSource;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private static final String TAG = EventViewModel.class.getSimpleName();
    // Common UI state fields
    private final MutableLiveData<String> errorMessageLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;

    public HomeViewModel() {
        errorMessageLiveData = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>();
    }

    public MutableLiveData<Result> getLastRace() {
        return WeeklyRaceRepository.getInstance().fetchLastWeeklyRace();
    }
    public MutableLiveData<Result> getNextRaceLiveData() {
        return WeeklyRaceRepository.getInstance().fetchNextWeeklyRace();
    }

    public LiveData<Result> getConstructorStandings() {
        return ConstructorStandingRepository.getInstance().fetchConstructorStanding();
    }

    public LiveData<Boolean> isLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public MutableLiveData<Result> getDriverStandingsLiveData() {
        return DriverStandingRepository.getInstance(new DriverStandingsRemoteDataSource()).fetchDriverStanding();
    }

    public MutableLiveData<Result> getConstructorStandingsLiveData() {
        return ConstructorStandingRepository.getInstance().fetchConstructorStanding();
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