package com.the_coffe_coders.fastestlap.ui.home.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorRepository;
import com.the_coffe_coders.fastestlap.repository.driver.DriverRepository;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.HomeViewModel;

public class HomeViewModelFactory implements ViewModelProvider.Factory {
    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;
    private final DriverRepository driverRepository;
    private final ConstructorRepository constructorRepository;

    public HomeViewModelFactory(RaceRepository raceRepository,
                                RaceResultRepository raceResultRepository,
                                DriverRepository driverRepository,
                                ConstructorRepository constructorRepository) {
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
