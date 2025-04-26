package com.the_coffe_coders.fastestlap.source.weeklyrace;

import com.the_coffe_coders.fastestlap.repository.weeklyrace.SingleWeeklyRaceCallback;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.WeeklyRacesCallback;

public interface WeeklyRaceDataSource {
    void getWeeklyRaces(WeeklyRacesCallback weeklyRacesCallback);

    void getNextRace(SingleWeeklyRaceCallback weeklyRaceCallback);

    void getLastRace(SingleWeeklyRaceCallback weeklyRaceCallback);
}
