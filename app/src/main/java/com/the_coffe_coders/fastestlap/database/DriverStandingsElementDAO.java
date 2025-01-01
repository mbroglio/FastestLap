package com.the_coffe_coders.fastestlap.database;

import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;

import java.util.List;

public interface DriverStandingsElementDAO {
    @Query("SELECT * FROM DriverStandingsElement")
    List<DriverStandingsElement> getAll();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DriverStandingsElement> standingsElements);
    @Query("DELETE from DriverStandingsElement")
    void deleteAll();
}
