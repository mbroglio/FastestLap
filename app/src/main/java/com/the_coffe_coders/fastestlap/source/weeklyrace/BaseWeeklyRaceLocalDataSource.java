package com.the_coffe_coders.fastestlap.source.weeklyrace;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceResponseCallback;

import java.util.List;

public abstract class BaseWeeklyRaceLocalDataSource {
    protected RaceResponseCallback raceCallback;

    public void setRaceCallback(RaceResponseCallback raceCallback) {
        this.raceCallback = raceCallback;
    }

    public abstract void getWeeklyRace();

    public abstract void insertWeeklyRaceList(List<WeeklyRace> weeklyRaceList);

}
