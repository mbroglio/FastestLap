package com.the_coffe_coders.fastestlap.source.junior.calendar;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.junior.JuniorCalendarDAO;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.repository.junior.calendar.JuniorCalendarCallback;

public class LocalJuniorCalendarDataSource implements JuniorCalendarDataSource {
    private static final String TAG = "LocalJuniorCalendarDataSource";
    private static LocalJuniorCalendarDataSource instance;
    private final JuniorCalendarDAO juniorCalendarDAO;

    public LocalJuniorCalendarDataSource(AppRoomDatabase appRoomDatabase) {
        this.juniorCalendarDAO = appRoomDatabase.juniorCalendarDAO();
    }

    public static synchronized LocalJuniorCalendarDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalJuniorCalendarDataSource(appRoomDatabase);
        }
        return instance;
    }

    @Override
    public void getJuniorCalendar(String series, JuniorCalendarCallback callback) {
        Log.d(TAG, "Fetching junior calendar from local database");
        JuniorCalendar calendar = juniorCalendarDAO.getBySeries(series);
        if (calendar != null) {
            callback.onCalendarLoaded(calendar);
        }else {
            callback.onError(new Exception("No junior calendar found in local database"));
        }
    }

    public void insertJuniorCalendar(JuniorCalendar calendar) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> juniorCalendarDAO.insert(calendar));
    }
}
