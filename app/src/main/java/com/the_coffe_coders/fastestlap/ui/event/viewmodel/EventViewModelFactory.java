package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;


import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;

public class EventViewModelFactory implements ViewModelProvider.Factory {
    private final RaceRepository raceRepository;

    public EventViewModelFactory(RaceRepository raceRepository) {
        this.raceRepository = raceRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(EventViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new EventViewModel(raceRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
