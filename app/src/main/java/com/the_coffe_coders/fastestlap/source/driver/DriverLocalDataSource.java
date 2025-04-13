package com.the_coffe_coders.fastestlap.source.driver;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.DriverDAO;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.repository.driver.DriverCallback;

public class DriverLocalDataSource implements DriverDataSource {
    private static DriverLocalDataSource instance;
    private final DriverDAO driverDAO;
    private static final String TAG = "DriverLocalDataSource";
    private DriverLocalDataSource(AppRoomDatabase appRoomDatabase) {
        this.driverDAO = appRoomDatabase.driverDAO();
    }
    public static synchronized DriverLocalDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new DriverLocalDataSource(appRoomDatabase);
        }
        return instance;
    }

    @Override
    public void getDriver(String driverId, DriverCallback callback) {
        Log.d(TAG, "Fetching driver with ID: " + driverId);
        callback.onDriverLoaded(driverDAO.getById(driverId));
    }

    public void insertDriver(Driver driver) {
        driverDAO.insertDriver(driver);
    }
}
