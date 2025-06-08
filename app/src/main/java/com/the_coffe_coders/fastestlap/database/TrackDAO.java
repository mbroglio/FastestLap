package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;

import java.util.List;

@Dao
public interface TrackDAO {
    @Query("SELECT * FROM Track WHERE trackId LIKE :id")
    Track getById(String id);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTrack(Track track);
}
