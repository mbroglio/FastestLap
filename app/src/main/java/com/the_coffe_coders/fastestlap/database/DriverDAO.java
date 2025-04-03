package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.List;

//TODO REMOVE ?
@Dao
public interface DriverDAO {
    @Query("SELECT * FROM Driver")
    List<Driver> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Driver> articles);

    @Query("SELECT * FROM Driver WHERE driverId LIKE :id")
    Driver getById(String id);

    @Query("DELETE from Driver")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDriver(Driver driver);//TAXI DRIVER

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertDriversList(List<Driver> newsList);

}


