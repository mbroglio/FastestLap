package com.the_coffe_coders.fastestlap.source.driver_standings;

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
                if (driverCallback != null) {
                    if (standings != null) {
                        Log.d(TAG, "Retrieved driver standings from local DB");
                        driverCallback.onSuccessFromLocal(standings);
                    } else {
                        Log.w(TAG, "No driver standings found in local DB");
                        driverCallback.onFailureFromLocal(new Exception("No driver standings found in database"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting driver standings", e);
                if (driverCallback != null) {
                    driverCallback.onFailureFromLocal(e);
                }
            }
        });
    }

    @Override
    public void insertDriversStandings(DriverStandings driverStandings) {
        if (driverStandings == null) {
            Log.w(TAG, "Attempted to insert null driver standings");
            if (driverCallback != null) {
                driverCallback.onFailureFromLocal(new IllegalArgumentException("Driver standings is null"));
            }
            return;
        }

        databaseExecutor.execute(() -> {
            try {
                Log.d(TAG, "Inserting driver standings into local DB");
                driverStandingsDAO.insert(driverStandings);
                sharedPreferencesUtil.writeStringData(
                        Constants.SHARED_PREFERENCES_FILENAME,
                        Constants.SHARED_PREFERENCES_LAST_UPDATE,
                        String.valueOf(System.currentTimeMillis())
                );

                if (driverCallback != null) {
                    driverCallback.onSuccessFromLocal(driverStandings);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting driver standings", e);
                if (driverCallback != null) {
                    driverCallback.onFailureFromLocal(e);
                }
            }
        });
    }
}