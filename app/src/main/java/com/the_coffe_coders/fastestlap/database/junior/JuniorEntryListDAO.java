package com.the_coffe_coders.fastestlap.database.junior;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.junior.JuniorEntryList;

@Dao
public interface JuniorEntryListDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(JuniorEntryList juniorEntryList);
    @Query("SELECT * FROM JuniorEntryList WHERE series = :series")
    JuniorEntryList getBySeries(String series);
}
