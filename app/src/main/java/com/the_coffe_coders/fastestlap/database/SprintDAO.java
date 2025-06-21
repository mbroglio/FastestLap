package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
@Dao
public interface SprintDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Race race);

    @Query("SELECT * FROM Race WHERE round = :round")
    Race getRaceByRound(int round);

    @Delete
    void delete(Race race);
}
