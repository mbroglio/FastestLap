package com.the_coffe_coders.fastestlap.database;
import static com.the_coffe_coders.fastestlap.util.Constants.DATABASE_VERSION;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Driver.class}, version = DATABASE_VERSION, exportSchema = false)//TODO set to true (export schema)
public abstract class DriverRoomDatabase extends RoomDatabase{
    public abstract DriverDAO driverDao();
    private static volatile DriverRoomDatabase INSTANCE;

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static DriverRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (DriverRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    DriverRoomDatabase.class, Constants.SAVED_DRIVERS_DATABASE)
                            .allowMainThreadQueries().build();
                }
            }
        }
        return INSTANCE;
    }
}


