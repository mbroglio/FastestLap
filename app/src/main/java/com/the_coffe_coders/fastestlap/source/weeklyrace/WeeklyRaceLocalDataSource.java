package com.the_coffe_coders.fastestlap.source.weeklyrace;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;

import com.the_coffe_coders.fastestlap.database.WeeklyRaceClassicDAO;
import com.the_coffe_coders.fastestlap.database.WeeklyRaceSprintDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceClassic;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceSprint;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.List;

public class WeeklyRaceLocalDataSource extends BaseWeeklyRaceLocalDataSource {
    private final WeeklyRaceClassicDAO weeklyRaceClassicDao;
    private final WeeklyRaceSprintDAO weeklyRaceSprintDao;
    private final SharedPreferencesUtils sharedPreferencesUtil;

    private static final String TAG = "WeeklyRaceLocalDataSource";

    public WeeklyRaceLocalDataSource(AppRoomDatabase appRoomDatabase, SharedPreferencesUtils sharedPreferencesUtil) {
        this.weeklyRaceClassicDao = appRoomDatabase.weeklyRaceClassicDAO();
        this.weeklyRaceSprintDao = appRoomDatabase.weeklyRaceSprintDAO();
        this.sharedPreferencesUtil = sharedPreferencesUtil;
    }

    @Override
    public void getWeeklyRaces() {
        Log.i(TAG, "getWeeklyRaces from local");
        List<WeeklyRace> weeklyRaceList = new ArrayList<>();
        weeklyRaceList.addAll(weeklyRaceClassicDao.getAllRaces());
        weeklyRaceList.addAll(weeklyRaceSprintDao.getAllRaces());

        raceCallback.onSuccessFromLocal(weeklyRaceList);
    }

    @Override
    public void insertWeeklyRaceList(List<WeeklyRace> weeklyRaceList) {
        Log.i(TAG, "insertWeeklyRaceList");
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            weeklyRaceList.forEach(weeklyRace -> {
                Log.i(TAG, "insertWeeklyRaceList: " + weeklyRace);
                if (weeklyRace instanceof WeeklyRaceClassic) {
                    weeklyRaceClassicDao.insert((WeeklyRaceClassic) weeklyRace);
                } else if (weeklyRace instanceof WeeklyRaceSprint) {
                    weeklyRaceSprintDao.insert((WeeklyRaceSprint) weeklyRace);
                }
            });
            sharedPreferencesUtil.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                    Constants.SHARED_PREFERENCES_LAST_UPDATE, String.valueOf(System.currentTimeMillis()));
            raceCallback.onSuccessFromLocal(weeklyRaceList);
        });
    }
}
