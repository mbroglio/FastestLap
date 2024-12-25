package com.the_coffe_coders.fastestlap.source.driver;

import com.the_coffe_coders.fastestlap.database.DriverDAO;
import com.the_coffe_coders.fastestlap.database.DriverRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.repository.driver.DriverResponseCallback;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import java.util.List;

public class DriverLocalDataSource extends BaseDriverLocalDataSource {
    private final DriverDAO driverDAO;
    private final SharedPreferencesUtils sharedPreferencesUtil;

    public DriverLocalDataSource(DriverRoomDatabase driverRoomDatabase, SharedPreferencesUtils sharedPreferencesUtil) {
        this.driverDAO = driverRoomDatabase.driverDao();
        this.sharedPreferencesUtil = sharedPreferencesUtil;
    }

    @Override
    public void getDrivers() {
        DriverRoomDatabase.databaseWriteExecutor.execute(() -> {
            driverCallback.onSuccessFromLocal(driverDAO.getAll());
        });
    }

    @Override
    public void getFavoriteDriver() {

    }

    @Override
    public void updateDriver(Driver driver) {

    }

    @Override
    public void insertDrivers(List<Driver> driverList) {
        DriverRoomDatabase.databaseWriteExecutor.execute(() -> {
            // Reads the news from the database
            List<Driver> allDrivers = driverDAO.getAll();

            if (driverList != null) {

                // Checks if the news just downloaded has already been downloaded earlier
                // in order to preserve the news status (marked as favorite or not)
                for (Driver driver : allDrivers) {
                    // This check works because News and NewsSource classes have their own
                    // implementation of equals(Object) and hashCode() methods
                    if (driverList.contains(driver)) {
                        // The primary key and the favorite status is contained only in the News objects
                        // retrieved from the database, and not in the News objects downloaded from the
                        // Web Service. If the same news was already downloaded earlier, the following
                        // line of code replaces the News object in newsList with the corresponding
                        // line of code replaces the News object in newsList with the corresponding
                        // News object saved in the database, so that it has the primary key and the
                        // favorite status.
                        driverList.set(driverList.indexOf(driver), driver);
                    }
                }

                // Writes the news in the database and gets the associated primary keys
                List<Long> insertedDriversIds = driverDAO.insertDriversList(driverList);
                for (int i = 0; i < driverList.size(); i++) {
                    // Adds the primary key to the corresponding object News just downloaded so that
                    // if the user marks the news as favorite (and vice-versa), we can use its id
                    // to know which news in the database must be marked as favorite/not favorite
                    driverList.get(i).setUid(insertedDriversIds.get(i));
                }

                sharedPreferencesUtil.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                        Constants.SHARED_PREFERENCES_LAST_UPDATE, String.valueOf(System.currentTimeMillis()));

                driverCallback.onSuccessFromLocal(driverList);
            }
        });
    }
}
