package com.the_coffe_coders.fastestlap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;

import java.util.List;

@Dao
public interface TrackDAO {
    @Query("SELECT * FROM Track")
    List<Track> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Track> articles);

    @Query("SELECT * FROM Track WHERE trackId LIKE :id")
    Track getById(String id);

    @Query("DELETE from Track")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTrack(Track track);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertTrackList(List<Track> newsList);
}
