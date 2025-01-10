package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;

import java.util.List;

@Dao
public interface RaceResultDAO {
    @Insert
    void insert(RaceResult raceResult);

    @Query("SELECT * FROM RaceResult WHERE uid = :id")
    RaceResult getRaceResultById(int id);

    @Query("SELECT * FROM RaceResult")
    List<RaceResult> getAllRaceResults();
}
