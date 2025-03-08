package com.the_coffe_coders.fastestlap.presentation.ui.event.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.data.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.data.repository.weeklyrace.RaceRepository;

public class EventViewModelFactory implements ViewModelProvider.Factory {
    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;

    public EventViewModelFactory(RaceRepository raceRepository, RaceResultRepository raceResultRepository) {
        this.raceRepository = raceRepository;
        this.raceResultRepository = raceResultRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(EventViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new EventViewModel(raceRepository, raceResultRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
