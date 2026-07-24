package com.the_coffe_coders.fastestlap.source.f1.driver;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.f1.DriverDAO;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.repository.f1.driver.DriverCallback;

public class LocalDriverDataSource implements DriverDataSource {
    private static final String TAG = "DriverLocalDataSource";
    private static LocalDriverDataSource instance;
    private final DriverDAO driverDAO;

    private LocalDriverDataSource(AppRoomDatabase appRoomDatabase) {
        this.driverDAO = appRoomDatabase.driverDAO();
    }

    public static synchronized LocalDriverDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalDriverDataSource(appRoomDatabase);
        }
        return instance;
    }

    @Override
    public void getDriver(String driverId, DriverCallback callback) {
        Log.d(TAG, "Fetching driver with ID: " + driverId);
        callback.onDriverLoaded(driverDAO.getById(driverId));
    }

    public void insertDriver(Driver driver) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> driverDAO.insertDriver(driver));
    }
}
