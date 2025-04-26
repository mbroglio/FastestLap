package com.the_coffe_coders.fastestlap.database;

import static com.the_coffe_coders.fastestlap.util.Constants.DATABASE_VERSION;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceClassic;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceSprint;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {DriverStandings.class, Driver.class, Constructor.class, ConstructorStandings.class, WeeklyRaceClassic.class, Race.class, WeeklyRaceSprint.class, RaceResult.class, Track.class, Nation.class}, version = DATABASE_VERSION, exportSchema = false)
//TODO set to true (export schema)
@TypeConverters({DatabaseConverters.class})
public abstract class AppRoomDatabase extends RoomDatabase {
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static volatile AppRoomDatabase INSTANCE;

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

    public abstract DriverStandingsDAO driverStandingsDao();

    public abstract DriverDAO driverDAO();

    public abstract ConstructorDAO constructorDAO();

    public abstract TrackDAO trackDAO();

    public abstract NationDAO nationDAO();

    public abstract ConstructorStandingsDAO constructorStandingsDao();

    public abstract WeeklyRaceClassicDAO weeklyRaceClassicDAO();

    public abstract WeeklyRaceSprintDAO weeklyRaceSprintDAO();

    public abstract RaceResultDAO raceResultDAO();

    public abstract RaceDAO raceDAO();
}


