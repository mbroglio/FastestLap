package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.WeeklyRaceRepository;

public class WeeklyRaceViewModelFactory implements ViewModelProvider.Factory {
    private final WeeklyRaceRepository weeklyRaceRepository;

    public WeeklyRaceViewModelFactory(Application application) {
        AppRoomDatabase appRoomDatabase = AppRoomDatabase.getDatabase(application);
        this.weeklyRaceRepository = WeeklyRaceRepository.getInstance(appRoomDatabase);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(WeeklyRaceViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new WeeklyRaceViewModel(weeklyRaceRepository);
            return viewModel;
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
