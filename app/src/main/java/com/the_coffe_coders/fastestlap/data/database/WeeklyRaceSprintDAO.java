package com.the_coffe_coders.fastestlap.data.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceSprint;

import java.util.List;

@Dao
public interface WeeklyRaceSprintDAO {
    @Insert
    void insert(WeeklyRaceSprint sprintRace);

    @Query("SELECT * FROM WeeklyRaceSprint WHERE uid = :id")
    WeeklyRaceSprint getRaceById(int id);

    @Query("SELECT * FROM WeeklyRaceSprint")
    List<WeeklyRaceSprint> getAllRaces();
}
