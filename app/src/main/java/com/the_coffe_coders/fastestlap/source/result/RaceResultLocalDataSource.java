package com.the_coffe_coders.fastestlap.source.result;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.RaceResultDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.List;

public class RaceResultLocalDataSource extends BaseRaceResultLocalDataSource {

    private static final String TAG = "RaceResultLocalDataSource";
    private final RaceResultDAO raceResultDao;
    private final SharedPreferencesUtils sharedPreferencesUtil;

    public RaceResultLocalDataSource(AppRoomDatabase appRoomDatabase, SharedPreferencesUtils sharedPreferencesUtil) {
        this.raceResultDao = appRoomDatabase.raceResultDAO();
        this.sharedPreferencesUtil = sharedPreferencesUtil;
    }

    @Override
    public void getRaceResults() {
        Log.i(TAG, "getRaceResults from local");
        List<RaceResult> raceResultsList = new ArrayList<>();
        raceResultsList.addAll(raceResultDao.getAllRaceResults());

        raceResultCallback.onSuccessFromLocal(raceResultsList);
    }

    @Override
    public void getAllRaceResult() {

    }

    public void getRaceResultById(int id) {
        Log.i(TAG, "getRaceResultById from local");
        RaceResult raceResult = raceResultDao.getRaceResultById(id);
        raceResultCallback.onSuccessFromLocal(raceResult);
    }

    @Override
    public void insertRaceResultList(List<RaceResult> raceResultList) {
        Log.i(TAG, "insertRaceResultList");
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            raceResultList.forEach(raceResult -> {
                Log.i(TAG, "insertRaceResultList: " + raceResult);
                raceResultDao.insert(raceResult);
            });
            sharedPreferencesUtil.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                    Constants.SHARED_PREFERENCES_LAST_UPDATE, String.valueOf(System.currentTimeMillis()));

            raceResultCallback.onSuccessFromLocal(raceResultList);
        });
    }
}
