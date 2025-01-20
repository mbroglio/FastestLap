package com.the_coffe_coders.fastestlap.database;

import androidx.room.Insert;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;

import java.util.List;

public interface RaceDAO {
    @Insert
    void insert(Race race);

    @Query("SELECT * FROM Race WHERE round = :round")
    Race getRaceByRound(int round);

    @Query("SELECT * FROM Race")
    List<Race> getAllRaces();
}
