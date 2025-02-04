package com.the_coffe_coders.fastestlap.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;

import java.util.List;

@Dao
public interface RaceDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Race race);

    // Allow batch insertions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Race> races);

    @Query("SELECT * FROM Race WHERE round = :round")
    Race getRaceByRound(int round);

    @Query("SELECT * FROM Race")
    List<Race> getAllRaces();

    // Additional useful queries
    @Query("SELECT * FROM Race ORDER BY round ASC")
    List<Race> getAllRacesSortedByRound();

    @Delete
    void delete(Race race);

    @Query("DELETE FROM Race")
    void deleteAll();

    // Optional: Add LiveData support if you're using it
    @Query("SELECT * FROM Race")
    LiveData<List<Race>> getAllRacesLive();
}