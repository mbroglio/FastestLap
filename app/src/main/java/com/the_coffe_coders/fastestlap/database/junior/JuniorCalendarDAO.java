package com.the_coffe_coders.fastestlap.database.junior;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;

@Dao
public interface JuniorCalendarDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(JuniorCalendar juniorCalendar);
    @Query("SELECT * FROM JuniorCalendar WHERE series = :series")
    JuniorCalendar getBySeries(String series);

    @Query("DELETE FROM JuniorCalendar WHERE series = :series")
    void deleteBySeries(String series);
}
