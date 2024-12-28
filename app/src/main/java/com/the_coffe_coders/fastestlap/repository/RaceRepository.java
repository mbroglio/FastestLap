package com.the_coffe_coders.fastestlap.repository;


import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import java.util.Collections;
import java.util.List;

public class RaceRepository implements IRaceRepository{
    private static final String TAG = "RetrofitRaceRepository";
    private RaceRepository() {

    }

    @Override
    public List<WeeklyRace> fetchWeeklyRace(long lastUpdate) {
        if(true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT)

        }else {

        }
        return Collections.emptyList();
    }

    @Override
    public WeeklyRace fetchNextRace(long lastUpdate) {
        return null;
    }

    @Override
    public WeeklyRace fetchLastRace(long lastUpdate) {
        return null;
    }
}
