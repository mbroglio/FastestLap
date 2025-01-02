package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;

import java.util.ArrayList;
import java.util.List;

public class EventViewModel extends ViewModel {

    private static final String TAG = EventViewModel.class.getSimpleName();
    private final RaceRepository raceRepository;
    private MutableLiveData<Result> eventLiveData;
    private MutableLiveData<Result> pastEventLiveData;
    private MutableLiveData<Result> upcomingEventLiveData;

    public EventViewModel(RaceRepository raceRepository) {
        this.raceRepository = raceRepository;
    }

    public MutableLiveData<Result> getEventLiveData() {
        if (eventLiveData == null) {
            eventLiveData = new MutableLiveData<>();
        }
        return eventLiveData;
    }

    public MutableLiveData<Result> getEventsLiveData(long lastUpdate) {
        if (eventLiveData == null) {
            eventLiveData = raceRepository.fetchWeeklyRaces(lastUpdate);
        }
        return eventLiveData;
    }

    public MutableLiveData<Result> getPastEventLiveData(long lastUpdate) {
        if (pastEventLiveData == null) {
            pastEventLiveData = new MutableLiveData<>();
        }
        return pastEventLiveData;
    }

    public MutableLiveData<Result> getUpcomingEventLiveData(long lastUpdate) {
        if (upcomingEventLiveData == null) {
            upcomingEventLiveData = new MutableLiveData<>();
        }
        return upcomingEventLiveData;
    }

    private List<WeeklyRace> extractUpcomingRaces(List<WeeklyRace> races) {
        List<WeeklyRace> upcomingRaces = new ArrayList<>();

        for (WeeklyRace weeklyRace : races) {
            // A Race is considered upcoming if it is yet to finish
            if (!weeklyRace.isWeekFinished()) {
                upcomingRaces.add(weeklyRace);
            }
        }

        return upcomingRaces;
    }

    private List<WeeklyRace> extractPastRaces(List<WeeklyRace> races) {
        List<WeeklyRace> pastRaces = new ArrayList<>();

        for (WeeklyRace weeklyRace : races) {
            // A Race is considered upcoming if it is yet to finish
            if (weeklyRace.isWeekFinished()) {
                pastRaces.add(weeklyRace);
            }
        }

        return pastRaces;
    }
}
