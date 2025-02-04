package com.the_coffe_coders.fastestlap.ui.home.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorStandingsRepository;
import com.the_coffe_coders.fastestlap.repository.driver.DriverStandingsRepository;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private static final String TAG = EventViewModel.class.getSimpleName();
    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;
    private final DriverStandingsRepository driverRepository;
    private final ConstructorStandingsRepository constructorRepository;
    private MutableLiveData<Result> upcomingEventLiveData;
    private MutableLiveData<Result> driver;
    private MutableLiveData<Result> constructor;
    private MutableLiveData<Result> driverStandings;
    private MutableLiveData<Result> constructorStanding;

    public HomeViewModel(RaceRepository raceRepository, RaceResultRepository raceResultRepository, DriverStandingsRepository driverRepository, ConstructorStandingsRepository constructorRepository) {
        this.raceRepository = raceRepository;
        this.raceResultRepository = raceResultRepository;
        this.driverRepository = driverRepository;
        this.constructorRepository = constructorRepository;
    }

    public MutableLiveData<Result> getLastRace(long lastUpdate) {
        // ServiceLocator.getInstance().getRaceRepository(getActivity().getApplication(), false).fetchLastRace(0);
        return raceRepository.fetchLastRace(0);
    }

    public MutableLiveData<Result> getNextRace(long lastUpdate) {
        //ServiceLocator.getInstance().getRaceRepository(getActivity().getApplication(), false).fetchNextRace(0);
        return raceRepository.fetchNextRace(0);
    }

    private void fetchDriverStandings(long lastUpdate) {
        this.driverStandings = driverRepository.fetchDriversStandings(lastUpdate);
    }

    public MutableLiveData<Result> getDriverStandingsLiveData(long lastUpdate) {
        if (driverStandings == null) {
            fetchDriverStandings(lastUpdate);
        }
        return driverStandings;
    }

    public DriverStandingsElement getDriverStandingsElement(List<DriverStandingsElement> driversList, String driverID) {
        for (DriverStandingsElement driver : driversList) {
            if (driver.getDriver().getDriverId().equals(driverID)) {
                return driver;
            }
        }

        return null;
    }


    public MutableLiveData<Result> getConstructorStandingsLiveData(long lastUpdate) {
        if (constructorStanding == null) {
            fetchConstructorStandings(lastUpdate);
        }
        return constructorStanding;
    }

    private void fetchConstructorStandings(long lastUpdate) {
        constructorStanding = constructorRepository.fetchConstructorStandings(lastUpdate);
    }


}
