package com.the_coffe_coders.fastestlap.data.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceClassic;

import java.util.List;

@Dao
public interface WeeklyRaceClassicDAO {
    @Insert
    void insert(WeeklyRaceClassic classicRace);

    @Query("SELECT * FROM WeeklyRaceClassic WHERE uid = :id")
    WeeklyRaceClassic getRaceById(int id);

    @Query("SELECT * FROM WeeklyRaceClassic")
    List<WeeklyRaceClassic> getAllRaces();
}
