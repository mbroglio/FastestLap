package com.the_coffe_coders.fastestlap.source.weeklyrace;

import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceResponseCallback;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.SingleWeeklyRaceCallback;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.WeeklyRacesCallback;

public abstract class BaseWeeklyRaceRemoteDataSource {
    protected RaceResponseCallback raceCallback;

    public void setRaceCallback(RaceResponseCallback raceCallback) {
        this.raceCallback = raceCallback;
    }

    public abstract void getWeeklyRaces();
    public abstract void getWeeklyRaces(WeeklyRacesCallback weeklyRacesCallback);
    public abstract void getNextRace();
    public abstract void getNextRace(SingleWeeklyRaceCallback weeklyRaceCallback);
    public abstract void getLastRace();
    public abstract void getLastRace(SingleWeeklyRaceCallback weeklyRaceCallback);
}
