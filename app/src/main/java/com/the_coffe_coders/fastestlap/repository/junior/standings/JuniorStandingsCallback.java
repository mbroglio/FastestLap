package com.the_coffe_coders.fastestlap.repository.junior.standings;

import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandings;

public interface JuniorStandingsCallback {
    void onDriverStandingsLoaded(JuniorDriverStandings driverStandings);

    void onConstructorStandingsLoaded(JuniorConstructorStandings constructorStandings);

    void onError(Exception e);
}
