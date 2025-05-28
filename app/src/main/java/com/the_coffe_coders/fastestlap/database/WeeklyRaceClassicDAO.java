package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceClassic;

import java.util.List;

@Dao
public interface WeeklyRaceClassicDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WeeklyRaceClassic classicRace);

    @Query("SELECT * FROM WeeklyRaceClassic WHERE uid = :id")
    WeeklyRaceClassic getRaceById(int id);

    @Query("SELECT DISTINCT * FROM WeeklyRaceClassic")
    List<WeeklyRaceClassic> getAllRaces();

    @Query("DELETE FROM WeeklyRaceClassic")
    void deleteAll();

    @Query("DELETE FROM WeeklyRaceClassic WHERE round LIKE :round")
    void delete(String round);
}
