package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;

@Dao
public interface ConstructorStandingsDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ConstructorStandings constructorStandings);

    @Query("SELECT * FROM ConstructorStandings")
    ConstructorStandings getConstructorStandings();
}
