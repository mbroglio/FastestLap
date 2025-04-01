package com.the_coffe_coders.fastestlap.source.weeklyrace;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

import java.util.List;

public abstract class BaseWeeklyRaceLocalDataSource implements WeeklyRaceDataSource {
    public abstract void getWeeklyRaces();
    public abstract void insertWeeklyRaceList(List<WeeklyRace> weeklyRaceList);
    public abstract void insertWeeklyRace(WeeklyRace weeklyRace);
}
