package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.standing.constructor.ConstructorStandingRepository;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class ConstructorStandingsViewModelFactory implements ViewModelProvider.Factory {
    private final ConstructorStandingRepository constructorStandingRepository;

    public ConstructorStandingsViewModelFactory(Application application) {
        AppRoomDatabase appRoomDatabase = ServiceLocator.getInstance().getRoomDatabase(application);
        this.constructorStandingRepository = ConstructorStandingRepository.getInstance(appRoomDatabase);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ConstructorStandingsViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new ConstructorStandingsViewModel(constructorStandingRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
