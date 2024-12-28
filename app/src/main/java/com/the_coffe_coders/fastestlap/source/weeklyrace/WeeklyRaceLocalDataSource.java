package com.the_coffe_coders.fastestlap.source.weeklyrace;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import java.util.List;

public class WeeklyRaceLocalDataSource extends BaseWeeklyRaceLocalDataSource {
    private final WeeklyRaceDao weeklyRaceDao;
    private final SharedPreferencesUtils sharedPreferencesUtil;

    public WeeklyRaceLocalDataSource(AppRoomDatabase appRoomDatabase, SharedPreferencesUtils sharedPreferencesUtil) {
        this.weeklyRaceDao = appRoomDatabase.weeklyRaceDao();
        this.sharedPreferencesUtil = sharedPreferencesUtil;
    }

    @Override
    public void getWeeklyRace() {

    }

    @Override
    public void insertWeeklyRaceList(List<WeeklyRace> weeklyRaceList) {

    }
}
