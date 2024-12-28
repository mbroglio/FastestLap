package com.the_coffe_coders.fastestlap.database;
import static com.the_coffe_coders.fastestlap.util.Constants.DATABASE_VERSION;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {DriverStandings.class, Driver.class}, version = DATABASE_VERSION, exportSchema = false)//TODO set to true (export schema)
@TypeConverters({DriverStandingsConverters.class})
public abstract class AppRoomDatabase extends RoomDatabase{
    public abstract DriverStandingsDAO driverStandingsDao();
    //public abstract DriverStandingsElementDAO driverStandingsElementDAO();
    public abstract DriverDAO driverDAO();
    private static volatile AppRoomDatabase INSTANCE;

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static AppRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppRoomDatabase.class, Constants.SAVED_DRIVERS_STANDINGS_DATABASE)
                            .allowMainThreadQueries().build();
                }
            }
        }
        return INSTANCE;
    }
}


