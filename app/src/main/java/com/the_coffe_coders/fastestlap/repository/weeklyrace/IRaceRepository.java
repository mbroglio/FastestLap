package com.the_coffe_coders.fastestlap.repository.weeklyrace;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

import java.util.List;

public interface IRaceRepository {
    List<WeeklyRace> fetchWeeklyRace(long lastUpdate);
    WeeklyRace fetchNextRace(long lastUpdate);
    WeeklyRace fetchLastRace(long lastUpdate);
}

