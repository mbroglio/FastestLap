package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.nation.NationRepository;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class NationViewModelFactory implements ViewModelProvider.Factory {
    private final NationRepository nationRepository;

    public NationViewModelFactory(NationRepository nationRepository) {
        this.nationRepository = nationRepository;
    }

    public NationViewModelFactory() {
        this.nationRepository = NationRepository.getInstance();
    }

    @NonNull
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(NationViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new NationViewModel(nationRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

}
