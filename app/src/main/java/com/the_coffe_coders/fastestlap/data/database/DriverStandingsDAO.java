package com.the_coffe_coders.fastestlap.data.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;

@Dao
public interface DriverStandingsDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DriverStandings driverStandings);

    @Query("SELECT * FROM DriverStandings")
    DriverStandings getDriverStandings();
}
