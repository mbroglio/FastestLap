package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorRepository;

public class ConstructorStandingsViewModelFactory implements ViewModelProvider.Factory {
    private final ConstructorRepository constructorRepository;

    public ConstructorStandingsViewModelFactory(ConstructorRepository constructorRepository)  {
        this.constructorRepository = constructorRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ConstructorStandingsViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new ConstructorStandingsViewModel(constructorRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
