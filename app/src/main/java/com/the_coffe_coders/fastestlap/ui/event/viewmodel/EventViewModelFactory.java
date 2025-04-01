package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class EventViewModelFactory implements ViewModelProvider.Factory {
    private final RaceResultRepository raceResultRepository;

    public EventViewModelFactory(Application application) {
        this.raceResultRepository = ServiceLocator.getInstance().getRaceResultRepository(application);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(EventViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new EventViewModel(raceResultRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
