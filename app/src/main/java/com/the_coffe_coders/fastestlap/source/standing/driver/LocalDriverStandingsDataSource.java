package com.the_coffe_coders.fastestlap.source.standing.driver;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.DriverStandingsDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingCallback;

public class LocalDriverStandingsDataSource implements DriverStandingDataSource {
    private static final String TAG = "DriverLocalDataSource";
    private static LocalDriverStandingsDataSource instance;
    private final DriverStandingsDAO driverStandingsDAO;

    public LocalDriverStandingsDataSource(AppRoomDatabase appRoomDatabase) {
        this.driverStandingsDAO = appRoomDatabase.driverStandingsDao();
    }

    public static synchronized LocalDriverStandingsDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalDriverStandingsDataSource(appRoomDatabase);
        }
        return instance;
    }

    @Override
    public void getDriverStandings(DriverStandingCallback callback) {
        Log.d(TAG, "Fetching driver standings from local database");
        // TODO: Check if the cast is right
        DriverStandings standings = driverStandingsDAO.get();
        if (standings != null) {
            callback.onDriverLoaded(standings);
        } else {
            callback.onError(new Exception("No driver standings found in local database"));
        }
    }

    public void insertDriverStandings(DriverStandings driverStandings) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            driverStandingsDAO.insert(driverStandings);
        });
    }
}