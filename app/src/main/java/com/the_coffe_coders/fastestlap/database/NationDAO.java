package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.nation.Nation;

import java.util.List;

@Dao
public interface NationDAO {
    @Query("SELECT * FROM Nation WHERE nationId LIKE :id")
    Nation getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNation(Nation nation);
}
