package com.the_coffe_coders.fastestlap.data.source.weeklyrace;

import com.the_coffe_coders.fastestlap.data.repository.weeklyrace.RaceResponseCallback;

public abstract class BaseWeeklyRaceRemoteDataSource {
    protected RaceResponseCallback raceCallback;

    public void setRaceCallback(RaceResponseCallback raceCallback) {
        this.raceCallback = raceCallback;
    }

    public abstract void getWeeklyRaces();

    public abstract void getNextRace();

    public abstract void getLastRace();

}
