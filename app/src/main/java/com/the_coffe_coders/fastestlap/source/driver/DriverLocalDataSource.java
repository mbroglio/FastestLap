package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.DriverStandingsDAO;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.repository.driver.DriverResponseCallback;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import java.util.List;

public class DriverLocalDataSource extends BaseDriverLocalDataSource {
    private final DriverStandingsDAO driverStandingsDAO;
    private final SharedPreferencesUtils sharedPreferencesUtil;

    public DriverLocalDataSource(AppRoomDatabase appRoomDatabase, SharedPreferencesUtils sharedPreferencesUtil) {
        this.driverStandingsDAO = appRoomDatabase.driverStandingsDao();
        this.sharedPreferencesUtil = sharedPreferencesUtil;
    }

    @Override
    public void getDrivers() {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            //driverCallback.onSuccessFromLocal(null);
        });
    }

    @Override
    public void getDriversStandings() {
        driverCallback.onSuccessFromLocal(driverStandingsDAO.getDriverStandings());
    }

    @Override
    public void getFavoriteDriver() {

    }

    @Override
    public void updateDriver(Driver driver) {

    }

    @Override
    public void insertDrivers(List<Driver> driverList) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            // Reads the news from the database
            List<Driver> allDrivers = null;

            if (driverList != null) {
                for (Driver driver : allDrivers) {
                    if (driverList.contains(driver)) {
                        driverList.set(driverList.indexOf(driver), driver);
                    }
                }

                List<Long> insertedDriversIds = null;
                for (int i = 0; i < driverList.size(); i++) {
                    driverList.get(i).setUid(insertedDriversIds.get(i));
                }

                sharedPreferencesUtil.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                        Constants.SHARED_PREFERENCES_LAST_UPDATE, String.valueOf(System.currentTimeMillis()));

                driverCallback.onSuccessFromLocal(driverList);
            }
        });
    }

    @Override
    public void insertDriversStandings(DriverStandings driverStandings) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            // Insert the DriverStandings in the database

            driverStandingsDAO.insert(driverStandings);
            if (driverStandings != null) {
                sharedPreferencesUtil.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                        Constants.SHARED_PREFERENCES_LAST_UPDATE, String.valueOf(System.currentTimeMillis()));

                driverCallback.onSuccessFromLocal(driverStandings);
            }
        });
    }
}
