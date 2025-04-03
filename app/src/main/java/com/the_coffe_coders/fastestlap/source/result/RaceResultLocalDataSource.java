package com.the_coffe_coders.fastestlap.source.result;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.RaceDAO;
import com.the_coffe_coders.fastestlap.database.RaceResultDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.List;

public class RaceResultLocalDataSource extends BaseRaceResultLocalDataSource {

    private static final String TAG = "RaceResultLocalDataSource";
    private final RaceResultDAO raceResultDao;
    private final RaceDAO raceDao;
    private final SharedPreferencesUtils sharedPreferencesUtil;

    public RaceResultLocalDataSource(AppRoomDatabase appRoomDatabase, SharedPreferencesUtils sharedPreferencesUtil) {
        this.raceResultDao = appRoomDatabase.raceResultDAO();
        this.sharedPreferencesUtil = sharedPreferencesUtil;
        this.raceDao = appRoomDatabase.raceDAO();
    }


    @Override
    public void getAllRaceResult() {
        Log.i(TAG, "getAllRaceResult from local");
        List<RaceResult> raceResultsList = new ArrayList<>(raceResultDao.getAllRaceResults());
        for (RaceResult raceResult : raceResultsList) {
            Log.i(TAG, "getAllRaceResult: " + raceResult);
        }
        System.out.println("size: " + raceResultsList.size());
        //raceResultCallback.onSuccessFromLocal(raceResultsList);
    }

    @Override
    public void getAllRaceList() {
        Log.i(TAG, "getAllRaceResult from local");
        List<Race> raceList = new ArrayList<>(raceDao.getAllRaces());
        for (Race raceResult : raceList) {
            Log.i(TAG, "getAllRaceResult: " + raceResult);
        }
        System.out.println("size: " + raceList.size());
        //raceResultCallback.onSuccessFromLocalRaceList(raceList);
    }


    public void getRaceResultById(int id) {
        Log.i(TAG, "getRaceResultById from local");
        RaceResult raceResult = raceResultDao.getRaceResultById(id);
        //raceResultCallback.onSuccessFromLocal(raceResult);
    }
/*
    @Override
    public synchronized void insertRaceList(List<Race> raceList) {
        Log.i(TAG, "insertRaceList");
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            raceList.forEach(race -> {
                Log.i(TAG, "insertRaceList: " + race);
                raceDao.insert(race);
            });
            sharedPreferencesUtil.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                    Constants.SHARED_PREFERENCES_LAST_UPDATE, String.valueOf(System.currentTimeMillis()));

            raceResultCallback.onSuccessFromLocalRaceList(raceList);
        });
    }
*/

    @Override
    public synchronized void insertRaceList(List<Race> raceList) {
        Log.i(TAG, "insertRaceList");
        // Create a new ArrayList to avoid concurrent modification
        final List<Race> racesToInsert = new ArrayList<>();
        racesToInsert.addAll(raceList);
        AppRoomDatabase.databaseWriteExecutor.execute(
                raceDao::deleteAll
        );
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Use traditional for loop instead of forEach to be more explicit about iteration
                for (Race race : racesToInsert) {
                    Log.i(TAG, "insertRaceList: " + race);
                    raceDao.insert(race);
                }

                sharedPreferencesUtil.writeStringData(
                        Constants.SHARED_PREFERENCES_FILENAME,
                        Constants.SHARED_PREFERENCES_LAST_UPDATE,
                        String.valueOf(System.currentTimeMillis())
                );
            } catch (Exception e) {
                Log.e(TAG, "Error inserting races: " + e.getMessage());
                // Consider adding error callback here
            }
        });
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

            //raceResultCallback.onSuccessFromLocal(raceResultList);
        });
    }
}
