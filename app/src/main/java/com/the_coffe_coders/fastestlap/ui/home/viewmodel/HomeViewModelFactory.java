package com.the_coffe_coders.fastestlap.ui.home.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorStandingsRepository;
import com.the_coffe_coders.fastestlap.repository.driver.DriverStandingsRepository;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;

public class HomeViewModelFactory implements ViewModelProvider.Factory {
    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;
    private final DriverStandingsRepository driverRepository;
    private final ConstructorStandingsRepository constructorRepository;

    public HomeViewModelFactory(RaceRepository raceRepository,
                                RaceResultRepository raceResultRepository,
                                DriverStandingsRepository driverRepository,
                                ConstructorStandingsRepository constructorRepository) {
        this.raceRepository = raceRepository;
        this.raceResultRepository = raceResultRepository;
        this.driverRepository = driverRepository;
        this.constructorRepository = constructorRepository;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(raceRepository, raceResultRepository, driverRepository, constructorRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
