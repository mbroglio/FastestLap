package com.the_coffe_coders.fastestlap.repository.junior.calendar;

import android.util.Log;

import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.source.junior.calendar.FirebaseJuniorCalendarDataSource;

public class JuniorCalendarRepository {
    private static final String TAG = "JuniorCalendarRepository";
    private static JuniorCalendarRepository instance;

    private final FirebaseJuniorCalendarDataSource firebaseJuniorCalendarDataSource;


    private JuniorCalendarRepository() {
        this.firebaseJuniorCalendarDataSource = FirebaseJuniorCalendarDataSource.getInstance();
    }

    public static synchronized JuniorCalendarRepository getInstance() {
        if (instance == null) {
            instance = new JuniorCalendarRepository();
        }
        return instance;
    }

    /*
    public void getCalendar(String series) {
        Log.i(TAG, "Fetching junior calendar from Firebase with series: " + series);

        firebaseJuniorCalendarDataSource.getJuniorCalendar(series, new JuniorCalendarCallback() {
            @Override
            public void onJuniorCalendarLoaded(JuniorCalendar calendar) {
                Log.i(TAG, "Successfully retrieved junior calendar from Firebase: " + calendar);

                if (calendar != null) {

                }
            }
        }

    }
    
     */
}
