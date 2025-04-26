package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingRepository;
import com.the_coffe_coders.fastestlap.source.standing.driver.DriverStandingsLocalDataSource;
import com.the_coffe_coders.fastestlap.source.standing.driver.DriverStandingsRemoteDataSource;

public class DriverStandingsViewModelFactory implements ViewModelProvider.Factory {
    private final DriverStandingRepository driverRepository;

    public DriverStandingsViewModelFactory(Application application) {
        AppRoomDatabase database = AppRoomDatabase.getDatabase(application);
        this.driverRepository = DriverStandingRepository.getInstance(new DriverStandingsRemoteDataSource(), new DriverStandingsLocalDataSource(database));
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
