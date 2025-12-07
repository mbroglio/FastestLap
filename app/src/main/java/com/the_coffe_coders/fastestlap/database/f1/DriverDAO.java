package com.the_coffe_coders.fastestlap.database.f1;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;

@Dao
public interface DriverDAO {
    @Query("SELECT * FROM Driver WHERE driverId LIKE :id")
    Driver getById(String id);

    @Query("DELETE from Driver")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDriver(Driver driver);//TAXI DRIVER
}


