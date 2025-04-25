package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

import java.util.List;

@Dao
public interface ConstructorDAO {
    @Query("SELECT * FROM Constructor")
    List<Constructor> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Constructor> articles);

    @Query("SELECT * FROM Constructor WHERE constructorId LIKE :id")
    Constructor getById(String id);

    @Query("DELETE from Constructor")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConstructor(Constructor constructor);//TAXI DRIVER

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertConstructorsList(List<Constructor> newsList);

}
