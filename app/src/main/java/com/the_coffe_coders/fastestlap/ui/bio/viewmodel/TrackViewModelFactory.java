package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.track.TrackRepository;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class TrackViewModelFactory implements ViewModelProvider.Factory {
    private final TrackRepository trackRepository;

    public TrackViewModelFactory(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public TrackViewModelFactory() {
        this.trackRepository = ServiceLocator.getInstance().getTrackRepository();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TrackViewModel.class)) {
            return (T) new TrackViewModel(trackRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
