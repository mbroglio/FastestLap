package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;

import java.util.List;
@Dao
public interface DriverStandingsDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(DriverStandings driverStandings);

    @Query("SELECT * FROM DriverStandings")
    DriverStandings getDriverStandings();
}
