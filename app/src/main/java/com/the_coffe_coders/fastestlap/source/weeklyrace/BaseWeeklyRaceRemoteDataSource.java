package com.the_coffe_coders.fastestlap.source.weeklyrace;

import com.the_coffe_coders.fastestlap.repository.weeklyrace.SingleWeeklyRaceCallback;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.WeeklyRacesCallback;

public abstract class BaseWeeklyRaceRemoteDataSource implements WeeklyRaceDataSource {
    @Override
    public abstract void getWeeklyRaces(WeeklyRacesCallback weeklyRacesCallback);

    @Override
    public abstract void getNextRace(SingleWeeklyRaceCallback weeklyRaceCallback);

    @Override
    public abstract void getLastRace(SingleWeeklyRaceCallback weeklyRaceCallback);
}
