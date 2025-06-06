package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.nation.NationRepository;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class NationViewModelFactory implements ViewModelProvider.Factory {
    private final NationRepository nationRepository;

    public NationViewModelFactory(Application application) {
        AppRoomDatabase appRoomDatabase = ServiceLocator.getInstance().getRoomDatabase(application);
        this.nationRepository = NationRepository.getInstance(appRoomDatabase, application.getApplicationContext());
    }

    @NonNull
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(NationViewModel.class)) {
            @SuppressWarnings("unchecked")
            T viewModel = (T) new NationViewModel(nationRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

}
