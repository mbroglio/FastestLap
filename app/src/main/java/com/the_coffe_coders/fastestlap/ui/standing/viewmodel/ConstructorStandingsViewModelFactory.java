package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.constructor.CommonConstructorRepository;

public class ConstructorStandingsViewModelFactory implements ViewModelProvider.Factory {
    private final CommonConstructorRepository commonConstructorRepository;

    public ConstructorStandingsViewModelFactory(CommonConstructorRepository commonConstructorRepository) {
        this.commonConstructorRepository = commonConstructorRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ConstructorStandingsViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new ConstructorStandingsViewModel(commonConstructorRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
