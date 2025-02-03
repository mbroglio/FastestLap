package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.driver.CommonDriverRepository;

public class DriverViewModelFactory implements ViewModelProvider.Factory{

    private final CommonDriverRepository driverRepository;

    public DriverViewModelFactory(CommonDriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @NonNull
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DriverViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new DriverViewModel(driverRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }


}
