package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorRepository;

public class ConstructorViewModelFactory implements ViewModelProvider.Factory {
    private final ConstructorRepository constructorRepository;

    public ConstructorViewModelFactory(ConstructorRepository constructorRepository) {
        this.constructorRepository = constructorRepository;
    }

    public ConstructorViewModelFactory(Application application) {
        this.constructorRepository = ConstructorRepository.getInstance(AppRoomDatabase.getDatabase(application), application.getApplicationContext());
    }

    @NonNull
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ConstructorViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new ConstructorViewModel(constructorRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
