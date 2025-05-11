package com.the_coffe_coders.fastestlap.source.standing.driver;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.DriverStandingsDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingCallback;

import java.util.concurrent.Executor;

public class LocalDriverStandingsDataSource {
    private static final String TAG = "DriverLocalDataSource";

    private final DriverStandingsDAO driverStandingsDAO;
    private final Executor databaseExecutor;

    public LocalDriverStandingsDataSource(AppRoomDatabase appRoomDatabase) {
        this.driverStandingsDAO = appRoomDatabase.driverStandingsDao();
        this.databaseExecutor = AppRoomDatabase.databaseWriteExecutor;
    }

    @Override
    public void getDriversStandings(DriverStandingCallback callback) {
        databaseExecutor.execute(() -> {
            try {
                DriverStandings standings = driverStandingsDAO.getDriverStandings();
                callback.onDriverLoaded(standings);
            } catch (Exception e) {
                Log.e(TAG, "Error getting driver standings", e);
                callback.onError(e);
            }
        });
    }

    @Override
    public void insertDriversStandings(DriverStandings driverStandings) {
        databaseExecutor.execute(() -> {
            try {
                Log.d(TAG, "Inserting driver standings into local DB");
                driverStandingsDAO.insert(driverStandings);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting driver standings", e);
            }
        });
    }
}