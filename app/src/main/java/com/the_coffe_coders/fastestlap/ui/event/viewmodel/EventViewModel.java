package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.result.ResultRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.WeeklyRaceRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
    TODO:
      - Extract last two methods and try to put them in a separate class i.e. RaceUtil or WeeklyRace
 */

public class EventViewModel extends ViewModel {

    private static final String TAG = EventViewModel.class.getSimpleName();
    private final RaceResultRepository raceResultRepository;

    public EventViewModel(RaceResultRepository raceResultRepository) {
        this.raceResultRepository = raceResultRepository;
    }

    public MutableLiveData<Result> getAllResults(int numberOfRaces) {
        return raceResultRepository.fetchAllRaceResults(0, numberOfRaces);
    }

    public MutableLiveData<Result> getRaceResults(String raceId) {
        System.out.println(raceId);
        return ResultRepository.getInstance().fetchResults(raceId);
    }

    public MutableLiveData<Result> getWeeklyRacesLiveData() {
        return WeeklyRaceRepository.getInstance().fetchWeeklyRaces();
    }

    public List<WeeklyRace> extractUpcomingRaces(List<WeeklyRace> races) {
        List<WeeklyRace> upcomingRaces = new ArrayList<>();
        for (WeeklyRace weeklyRace : races) {
            // A Race is considered upcoming if it is yet to finish
            if (!weeklyRace.isWeekFinished()) {
                upcomingRaces.add(weeklyRace);
            }
        }
        upcomingRaces.sort(Comparator.comparingInt(race -> Integer.parseInt(race.getRound())));
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
