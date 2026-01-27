package com.the_coffe_coders.fastestlap.database.f1;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;

import java.util.List;

@Dao
public interface DriverStandingsDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DriverStandings driverStandings);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<Driver> driverStandings);

    @Query("SELECT * FROM DriverStandings")
    DriverStandings get();

    @Query("SELECT * FROM Driver")
    List<Driver> getDrivers();
}
