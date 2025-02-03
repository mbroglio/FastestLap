package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.constructor.CommonConstructorRepository;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class ConstructorViewModelFactory implements ViewModelProvider.Factory {
    private final CommonConstructorRepository constructorRepository;

    public ConstructorViewModelFactory(CommonConstructorRepository constructorRepository) {
        this.constructorRepository = constructorRepository;
    }

    public ConstructorViewModelFactory() {
        this.constructorRepository = ServiceLocator.getInstance().getCommonConstructorRepository();
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
