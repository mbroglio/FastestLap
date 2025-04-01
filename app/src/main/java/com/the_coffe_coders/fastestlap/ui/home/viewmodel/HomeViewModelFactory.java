package com.the_coffe_coders.fastestlap.ui.home.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;

public class HomeViewModelFactory implements ViewModelProvider.Factory {
    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;


    public HomeViewModelFactory(RaceRepository raceRepository,
                                RaceResultRepository raceResultRepository) {
        this.raceRepository = raceRepository;
        this.raceResultRepository = raceResultRepository;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(raceRepository, raceResultRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
