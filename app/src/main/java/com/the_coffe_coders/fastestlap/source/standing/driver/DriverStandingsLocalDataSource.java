package com.the_coffe_coders.fastestlap.source.standing.driver;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.DriverStandingsDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import java.util.concurrent.Executor;

public class DriverStandingsLocalDataSource extends BaseDriverStandingsLocalDataSource {
    private static final String TAG = "DriverLocalDataSource";

    private final DriverStandingsDAO driverStandingsDAO;
    private final SharedPreferencesUtils sharedPreferencesUtil;
    private final Executor databaseExecutor;

    public DriverStandingsLocalDataSource(AppRoomDatabase appRoomDatabase, SharedPreferencesUtils sharedPreferencesUtil) {
        this.driverStandingsDAO = appRoomDatabase.driverStandingsDao();
        this.sharedPreferencesUtil = sharedPreferencesUtil;
        this.databaseExecutor = AppRoomDatabase.databaseWriteExecutor;
    }

    @Override
    public void getDriversStandings() {
        databaseExecutor.execute(() -> {
            try {
                DriverStandings standings = driverStandingsDAO.getDriverStandings();
                if (true) {//callback != null
                    if (standings != null) {
                        Log.d(TAG, "Retrieved driver standings from local DB");
                        //success
                    } else {
                        Log.w(TAG, "No driver standings found in local DB");
                        //failure
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting driver standings", e);
                //failure
            }
        });
    }

    @Override
    public void insertDriversStandings(DriverStandings driverStandings) {
        databaseExecutor.execute(() -> {
            try {
                Log.d(TAG, "Inserting driver standings into local DB");
                driverStandingsDAO.insert(driverStandings);
                sharedPreferencesUtil.writeStringData(
                        Constants.SHARED_PREFERENCES_FILENAME,
                        Constants.SHARED_PREFERENCES_LAST_UPDATE,
                        String.valueOf(System.currentTimeMillis())
                );

                //success from local
            } catch (Exception e) {
                Log.e(TAG, "Error inserting driver standings", e);
                //failure from local
            }
        });
    }
}