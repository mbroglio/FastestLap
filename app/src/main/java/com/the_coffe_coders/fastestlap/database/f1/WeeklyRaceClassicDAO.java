package com.the_coffe_coders.fastestlap.database.f1;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRaceClassic;

import java.util.List;

@Dao
public interface WeeklyRaceClassicDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WeeklyRaceClassic classicRace);
    @Query("SELECT DISTINCT * FROM WeeklyRaceClassic")
    List<WeeklyRaceClassic> getAllRaces();

    @Query("DELETE FROM WeeklyRaceClassic")
    void deleteAll();

    @Query("DELETE FROM WeeklyRaceClassic WHERE round LIKE :round")
    void delete(String round);
}
