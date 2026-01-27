package com.the_coffe_coders.fastestlap.source.junior.standings;

import com.the_coffe_coders.fastestlap.repository.junior.standings.JuniorStandingsCallback;

public interface JuniorStandingsDataSource {

    void getJuniorDriverStandings(String series, JuniorStandingsCallback callback);

    void getJuniorConstructorStandings(String series, JuniorStandingsCallback callback);
}
