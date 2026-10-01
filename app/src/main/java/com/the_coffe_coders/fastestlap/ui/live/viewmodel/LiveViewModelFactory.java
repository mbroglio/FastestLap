package com.the_coffe_coders.fastestlap.ui.live.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.repository.f1.livetiming.LiveTimingRepository;

/**
 * Factory per {@link LiveViewModel}.
 *
 * <p>Segue lo stesso pattern delle altre factory del progetto
 * (es. {@code WeeklyRaceViewModelFactory}): riceve l'Application e
 * istanzia il repository con {@code getInstance()}.</p>
 */
public class LiveViewModelFactory implements ViewModelProvider.Factory {

    private final LiveTimingRepository liveTimingRepository;

    public LiveViewModelFactory(Application application) {
        this.liveTimingRepository = LiveTimingRepository.getInstance(application);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LiveViewModel.class)) {
            @SuppressWarnings("unchecked")
            T viewModel = (T) new LiveViewModel(liveTimingRepository);
            return viewModel;
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
