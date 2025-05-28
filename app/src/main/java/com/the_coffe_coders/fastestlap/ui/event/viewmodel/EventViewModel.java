package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResultFastestLap;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EventViewModel extends ViewModel {

    private static final String TAG = EventViewModel.class.getSimpleName();

    public EventViewModel() {
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

    public RaceResultFastestLap extractFastestLap(List<RaceResult> results) {
        RaceResultFastestLap fastestLap = new RaceResultFastestLap();
        for (RaceResult result : results) {
            if (result.getFastestLap().getRank().equals("1")) {
                fastestLap = result.getFastestLap();
                fastestLap.setDriverName(result.getDriver().getFullName());
                fastestLap.setConstructorId(result.getConstructor().getConstructorId());
                break;
            }
        }
        return fastestLap;
    }
}
