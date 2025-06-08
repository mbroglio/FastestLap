package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceSprint;

import java.util.List;

@Dao
public interface WeeklyRaceSprintDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WeeklyRaceSprint sprintRace);
    @Query("SELECT * FROM WeeklyRaceSprint")
    List<WeeklyRaceSprint> getAllRaces();

    @Query("DELETE FROM WeeklyRaceSprint")
    void deleteAll();

    @Query("DELETE FROM WeeklyRaceSprint WHERE round LIKE :round")
    void delete(String round);
}
