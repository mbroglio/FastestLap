package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.driver.DriverStandingsStandingsRepository;

public class DriverStandingsViewModelFactory implements ViewModelProvider.Factory {
    private final DriverStandingsStandingsRepository driverRepository;

    public DriverStandingsViewModelFactory(DriverStandingsStandingsRepository driverRepository) {
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
