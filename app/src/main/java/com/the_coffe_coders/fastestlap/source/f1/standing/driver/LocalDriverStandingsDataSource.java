package com.the_coffe_coders.fastestlap.source.f1.standing.driver;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.f1.DriverStandingsDAO;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;
import com.the_coffe_coders.fastestlap.repository.f1.standing.driver.DriverStandingCallback;

import java.util.List;

public class LocalDriverStandingsDataSource implements DriverStandingDataSource {
    private static final String TAG = "DriverLocalDataSource";
    private static LocalDriverStandingsDataSource instance;
    private final DriverStandingsDAO driverStandingsDAO;
    private final AppRoomDatabase appRoomDatabase;

    public LocalDriverStandingsDataSource(AppRoomDatabase appRoomDatabase) {
        this.appRoomDatabase = appRoomDatabase;
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
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                DriverStandings standings = driverStandingsDAO.get();
                if (standings != null) {
                    callback.onDriverStandingsLoaded(standings);
                } else {
                    getDriversList(callback);
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    @Override
    public void getDriversList(DriverStandingCallback callback) {
        Log.d(TAG, "Fetching drivers list from local database");
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<Driver> drivers = driverStandingsDAO.getDrivers();
                if (drivers != null) {
                    callback.onDriverListLoaded(drivers);
                } else {
                    callback.onError(new Exception("No drivers found in local database"));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void insertDriverStandings(DriverStandings driverStandings) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> driverStandingsDAO.insert(driverStandings));
    }

    public void insertDriverList(List<Driver> driverList) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> driverStandingsDAO.insert(driverList));
    }
}