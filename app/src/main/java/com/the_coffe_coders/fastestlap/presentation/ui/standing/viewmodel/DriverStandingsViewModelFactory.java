package com.the_coffe_coders.fastestlap.presentation.ui.standing.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.data.repository.standings.DriverStandingsRepository;

public class DriverStandingsViewModelFactory implements ViewModelProvider.Factory {
    private final DriverStandingsRepository driverRepository;

    public DriverStandingsViewModelFactory(DriverStandingsRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DriverStandingsViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new DriverStandingsViewModel(driverRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
