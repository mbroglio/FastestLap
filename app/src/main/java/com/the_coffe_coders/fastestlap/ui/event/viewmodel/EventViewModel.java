package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;

import java.util.ArrayList;
import java.util.List;

/*
    TODO:
      - Extract last two methods and try to put them in a separate class i.e. RaceUtil or WeeklyRace
 */

public class EventViewModel extends ViewModel {

    private static final String TAG = EventViewModel.class.getSimpleName();
    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;

    private MutableLiveData<Result> eventLiveData;
    private MutableLiveData<Result> pastEventLiveData;
    private MutableLiveData<Result> upcomingEventLiveData;

    public EventViewModel(RaceRepository raceRepository, RaceResultRepository raceResultRepository) {
        this.raceRepository = raceRepository;
        this.raceResultRepository = raceResultRepository;
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

    public MutableLiveData<Result> getAllResults() {
        return raceResultRepository.fetchAllRaceResults(0);
    }


    public MutableLiveData<Result> getRaceResults(long lastUpdate, String raceId) {
        System.out.println(raceId);
        return raceResultRepository.fetchRaceResult(Integer.parseInt(raceId), 0L);
    }

    public MutableLiveData<Result> getPastEventLiveData(long lastUpdate) {
        return raceRepository.fetchWeeklyRaces(0);
    }

    public MutableLiveData<Result> getUpcomingEventLiveData(long lastUpdate) {
        return raceRepository.fetchWeeklyRaces(0);

    }

    public List<WeeklyRace> extractUpcomingRaces(List<WeeklyRace> races) {
        List<WeeklyRace> upcomingRaces = new ArrayList<>();

        for (WeeklyRace weeklyRace : races) {
            // A Race is considered upcoming if it is yet to finish
            if (!weeklyRace.isWeekFinished()) {
                upcomingRaces.add(weeklyRace);
            }
        }

        return upcomingRaces;
    }

    public List<WeeklyRace> extractPastRaces(List<WeeklyRace> races) {
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
