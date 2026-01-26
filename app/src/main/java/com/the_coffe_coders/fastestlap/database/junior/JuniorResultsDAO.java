package com.the_coffe_coders.fastestlap.database.junior;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;

@Dao
public interface JuniorResultsDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(JuniorResult juniorResult);
    @Query("SELECT * FROM JuniorResult WHERE series = :series")
    JuniorResult getBySeries(String series);
}
