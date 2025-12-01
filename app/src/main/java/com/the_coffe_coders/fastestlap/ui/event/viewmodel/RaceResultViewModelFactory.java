package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.result.ResultRepository;

public class RaceResultViewModelFactory implements ViewModelProvider.Factory {
    private final ResultRepository resultRepository;

    public RaceResultViewModelFactory(Application application, Context context) {
        AppRoomDatabase appRoomDatabase = AppRoomDatabase.getDatabase(application);
        this.resultRepository = ResultRepository.getInstance(appRoomDatabase, context);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(RaceResultViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new RaceResultViewModel(resultRepository);
            return viewModel;
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}