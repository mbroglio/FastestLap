package com.the_coffe_coders.fastestlap.database.junior;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandings;

@Dao
public interface JuniorStandingsDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConstructorStandings(JuniorConstructorStandings juniorConstructorStandings);

    @Query("SELECT * FROM JuniorConstructorStandings WHERE series = :series")
    JuniorConstructorStandings getConstructorStandingsBySeries(String series);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDriverStandings(JuniorDriverStandings juniorDriverStandings);

    @Query("SELECT * FROM JuniorDriverStandings WHERE series = :series")
    JuniorDriverStandings getDriverStandingsBySeries(String series);

    @Query("DELETE FROM JuniorConstructorStandings WHERE series = :series")
    void deleteConstructorStandingsBySeries(String series);

    @Query("DELETE FROM JuniorDriverStandings WHERE series = :series")
    void deleteDriverStandingsBySeries(String series);

}
