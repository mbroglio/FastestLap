package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.standings.driver.DriverStandingRepository;
import com.the_coffe_coders.fastestlap.repository.standings.driver.DriverStandingsRepository;
import com.the_coffe_coders.fastestlap.source.driver_standings.DriverStandingsRemoteDataSource;

public class DriverStandingsViewModelFactory implements ViewModelProvider.Factory {
    private final DriverStandingRepository driverRepository;

    public DriverStandingsViewModelFactory() {
        this.driverRepository = DriverStandingRepository.getInstance(new DriverStandingsRemoteDataSource());
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
