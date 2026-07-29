package com.the_coffe_coders.fastestlap.source.f1.weeklyrace;

import com.the_coffe_coders.fastestlap.repository.f1.weeklyrace.SingleWeeklyRaceCallback;
import com.the_coffe_coders.fastestlap.repository.f1.weeklyrace.WeeklyRacesCallback;

public interface WeeklyRaceDataSource {
    void getWeeklyRaces(WeeklyRacesCallback weeklyRacesCallback);

    void getNextRace(SingleWeeklyRaceCallback weeklyRaceCallback);

    void getLastRace(SingleWeeklyRaceCallback weeklyRaceCallback);
}
