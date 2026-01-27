package com.the_coffe_coders.fastestlap.database;

import static com.the_coffe_coders.fastestlap.util.Constants.DATABASE_VERSION;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.the_coffe_coders.fastestlap.database.f1.ConstructorDAO;
import com.the_coffe_coders.fastestlap.database.f1.ConstructorStandingsDAO;
import com.the_coffe_coders.fastestlap.database.f1.DriverDAO;
import com.the_coffe_coders.fastestlap.database.f1.DriverStandingsDAO;
import com.the_coffe_coders.fastestlap.database.f1.QualifyingDAO;
import com.the_coffe_coders.fastestlap.database.f1.RaceDAO;
import com.the_coffe_coders.fastestlap.database.f1.SprintDAO;
import com.the_coffe_coders.fastestlap.database.f1.WeeklyRaceClassicDAO;
import com.the_coffe_coders.fastestlap.database.f1.WeeklyRaceSprintDAO;
import com.the_coffe_coders.fastestlap.database.junior.JuniorCalendarDAO;
import com.the_coffe_coders.fastestlap.database.junior.JuniorEntryListDAO;
import com.the_coffe_coders.fastestlap.database.junior.JuniorResultsDAO;
import com.the_coffe_coders.fastestlap.database.junior.JuniorStandingsDAO;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRaceClassic;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRaceSprint;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorEntryList;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {DriverStandings.class, Driver.class, Constructor.class, ConstructorStandings.class,
        WeeklyRaceClassic.class, Race.class, WeeklyRaceSprint.class, RaceResult.class, Track.class, Nation.class,
        JuniorCalendar.class, JuniorEntryList.class, JuniorResult.class, JuniorDriverStandings.class,
        JuniorConstructorStandings.class}, version = DATABASE_VERSION, exportSchema = false)
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

    public abstract RaceDAO raceDAO();

    public abstract QualifyingDAO qualifyingDAO();

    public abstract SprintDAO sprintDAO();

    public abstract JuniorCalendarDAO juniorCalendarDAO();

    public abstract JuniorEntryListDAO juniorEntryListDAO();

    public abstract JuniorResultsDAO juniorResultsDAO();

    public abstract JuniorStandingsDAO juniorStandingsDAO();

}


