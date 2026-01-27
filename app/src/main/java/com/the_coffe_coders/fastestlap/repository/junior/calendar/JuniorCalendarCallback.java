package com.the_coffe_coders.fastestlap.repository.junior.calendar;

import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;

public interface JuniorCalendarCallback {
    void onCalendarLoaded(JuniorCalendar calendar);

    void onError(Exception e);
}
