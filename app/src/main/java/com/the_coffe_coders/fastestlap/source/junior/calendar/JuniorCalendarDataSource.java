package com.the_coffe_coders.fastestlap.source.junior.calendar;

import com.the_coffe_coders.fastestlap.repository.junior.calendar.JuniorCalendarCallback;

public interface JuniorCalendarDataSource {
    void getJuniorCalendar(String series, JuniorCalendarCallback callback);
}
