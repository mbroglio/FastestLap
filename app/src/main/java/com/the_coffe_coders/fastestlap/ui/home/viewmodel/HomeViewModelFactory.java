package com.the_coffe_coders.fastestlap.ui.home.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class HomeViewModelFactory implements ViewModelProvider.Factory {
    public HomeViewModelFactory(Application application) {}

    @NonNull
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel();
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
