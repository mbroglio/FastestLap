package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;
import com.the_coffe_coders.fastestlap.util.Constants;

import org.threeten.bp.ZonedDateTime;

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
        ZonedDateTime now = ZonedDateTime.now();

        List<WeeklyRace> upcomingRaces = new ArrayList<>();

        for (WeeklyRace weeklyRace : races) {
            ZonedDateTime raceDateTime = weeklyRace.getDateTime();
            raceDateTime = raceDateTime.plusMinutes(Constants.SESSION_DURATION.get("Race"));

            // A Race is considered upcoming if it is yet to finish
            if (now.isBefore(raceDateTime)) {
                upcomingRaces.add(weeklyRace);
            }
        }

        return upcomingRaces;
    }

}
