package com.the_coffe_coders.fastestlap.data.database;

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

    @Query("DELETE from Driver")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertDriversList(List<Driver> newsList);

}


