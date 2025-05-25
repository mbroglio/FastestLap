package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.WeeklyRaceRepository;

public class WeeklyRaceViewModel extends ViewModel {
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final WeeklyRaceRepository weeklyRaceRepository;

    public WeeklyRaceViewModel(WeeklyRaceRepository weeklyRaceRepository) {
        this.weeklyRaceRepository = weeklyRaceRepository;
    }

    public MutableLiveData<Result> getLastRace() {
        return weeklyRaceRepository.fetchLastWeeklyRace();
    }

    public MutableLiveData<Result> getNextRaceLiveData() {
        return weeklyRaceRepository.fetchNextWeeklyRace();
    }

    public MutableLiveData<Result> getWeeklyRacesLiveData() {
        return weeklyRaceRepository.fetchWeeklyRaces();
    }
}
