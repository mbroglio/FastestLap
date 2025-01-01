package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.driver.DriverRepository;

public class DriverStandingsViewModelFactory implements ViewModelProvider.Factory {
    private final DriverRepository driverRepository;

    public DriverStandingsViewModelFactory(DriverRepository driverRepository) {
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
