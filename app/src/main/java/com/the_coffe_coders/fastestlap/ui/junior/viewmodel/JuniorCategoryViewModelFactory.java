package com.the_coffe_coders.fastestlap.ui.junior.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.junior.calendar.JuniorCalendarRepository;
import com.the_coffe_coders.fastestlap.repository.junior.entrylist.JuniorEntryListRepository;
import com.the_coffe_coders.fastestlap.repository.junior.result.JuniorResultRepository;
import com.the_coffe_coders.fastestlap.repository.junior.standings.JuniorStandingsRepository;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class JuniorCategoryViewModelFactory implements ViewModelProvider.Factory {
    private final JuniorCalendarRepository juniorCalendarRepository;
    private final JuniorEntryListRepository juniorEntryListRepository;
    private final JuniorResultRepository juniorResultRepository;
    private final JuniorStandingsRepository juniorStandingsRepository;


    public JuniorCategoryViewModelFactory(Application application) {
        AppRoomDatabase appRoomDatabase = ServiceLocator.getInstance().getRoomDatabase(application);
        this.juniorCalendarRepository = JuniorCalendarRepository.getInstance(appRoomDatabase, application.getApplicationContext());
        this.juniorEntryListRepository = JuniorEntryListRepository.getInstance(appRoomDatabase, application.getApplicationContext());
        this.juniorResultRepository = JuniorResultRepository.getInstance(appRoomDatabase, application.getApplicationContext());
        this.juniorStandingsRepository = JuniorStandingsRepository.getInstance(appRoomDatabase, application.getApplicationContext());
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(JuniorCategoryViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new JuniorCategoryViewModel(juniorCalendarRepository, juniorEntryListRepository, juniorResultRepository, juniorStandingsRepository);
            return viewModel;
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
