package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.track.TrackRepository;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class TrackViewModelFactory implements ViewModelProvider.Factory {
    private final TrackRepository trackRepository;

    public TrackViewModelFactory(Application application) {
        AppRoomDatabase appRoomDatabase = ServiceLocator.getInstance().getRoomDatabase(application);
        this.trackRepository = TrackRepository.getInstance(appRoomDatabase, application.getApplicationContext());
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TrackViewModel.class)) {
            @SuppressWarnings("unchecked")
            T viewModel = (T) new TrackViewModel(trackRepository);
            return viewModel;
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
