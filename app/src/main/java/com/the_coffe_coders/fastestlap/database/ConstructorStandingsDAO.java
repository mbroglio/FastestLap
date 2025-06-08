package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;

import java.util.List;

@Dao
public interface ConstructorStandingsDAO {
    @Query("SELECT * FROM ConstructorStandings")
    ConstructorStandings get();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ConstructorStandings> constructorStandingsList);

    @Query("DELETE FROM ConstructorStandings")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ConstructorStandings constructorStandings);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertConstructorStandingsList(List<ConstructorStandings> constructorStandingsList);
}