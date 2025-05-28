package com.the_coffe_coders.fastestlap.source.weeklyrace;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.WeeklyRaceClassicDAO;
import com.the_coffe_coders.fastestlap.database.WeeklyRaceSprintDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceClassic;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceSprint;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.SingleWeeklyRaceCallback;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.WeeklyRacesCallback;

import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

public class LocalWeeklyRaceDataSource {
    private static final String TAG = "LocalWeeklyRaceDataSource";
    private static LocalWeeklyRaceDataSource instance;
    private final WeeklyRaceClassicDAO weeklyRaceClassicDao;
    private final WeeklyRaceSprintDAO weeklyRaceSprintDao;

    private LocalWeeklyRaceDataSource(AppRoomDatabase appRoomDatabase) {
        this.weeklyRaceClassicDao = appRoomDatabase.weeklyRaceClassicDAO();
        this.weeklyRaceSprintDao = appRoomDatabase.weeklyRaceSprintDAO();
    }

    public static synchronized LocalWeeklyRaceDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalWeeklyRaceDataSource(appRoomDatabase);
        }
        return instance;
    }

    public void getWeeklyRaces(WeeklyRacesCallback callback) {
        Log.d(TAG, "Fetching all weekly races from local database");
        try {
            List<WeeklyRace> weeklyRaceList = new ArrayList<>();
            weeklyRaceList.addAll(weeklyRaceClassicDao.getAllRaces());
            weeklyRaceList.addAll(weeklyRaceSprintDao.getAllRaces());

            if (weeklyRaceList != null && !weeklyRaceList.isEmpty()) {
                Log.d(TAG, "Found " + weeklyRaceList.size() + " weekly races in local database");
                callback.onSuccess(weeklyRaceList);
            } else {
                Log.d(TAG, "No weekly races found in local database");
                callback.onFailure(new Exception("No weekly races found in local database"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving weekly races from database: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    public void saveWeeklyRaces(List<WeeklyRace> weeklyRaces) {
        Log.d(TAG, "Saving weekly races to local database. Count: " + weeklyRaces.size());
        try {
            weeklyRaceClassicDao.deleteAll();
            weeklyRaceSprintDao.deleteAll();
            for (WeeklyRace weeklyRace : weeklyRaces) {
                if (weeklyRace instanceof WeeklyRaceClassic) {
                    weeklyRaceClassicDao.insert((WeeklyRaceClassic) weeklyRace);
                } else if (weeklyRace instanceof WeeklyRaceSprint) {
                    weeklyRaceSprintDao.insert((WeeklyRaceSprint) weeklyRace);
                }
            }
            Log.d(TAG, "Weekly races successfully saved to local database");
        } catch (Exception e) {
            Log.e(TAG, "Error saving weekly races to database: " + e.getMessage());
        }
    }

    public void getNextRace(SingleWeeklyRaceCallback callback) {
        Log.d(TAG, "Fetching next weekly race from local database");
        try {
            // Get all races and filter for the next one (upcoming race with earliest date)
            List<WeeklyRaceClassic> classicRaces = weeklyRaceClassicDao.getAllRaces();
            List<WeeklyRaceSprint> sprintRaces = weeklyRaceSprintDao.getAllRaces();

            WeeklyRace nextRace = null;
            LocalDateTime currentDateTime = LocalDateTime.now();
            LocalDateTime closestDateTime = null;

            // Find the next classic race
            for (WeeklyRaceClassic race : classicRaces) {
                LocalDateTime raceDateTime = race.getDateTime();
                if (raceDateTime != null && raceDateTime.isAfter(currentDateTime)) {
                    if (closestDateTime == null || raceDateTime.isBefore(closestDateTime)) {
                        closestDateTime = raceDateTime;
                        nextRace = race;
                    }
                }
            }

            // Find the next sprint race
            for (WeeklyRaceSprint race : sprintRaces) {
                LocalDateTime raceDateTime = race.getDateTime();
                if (raceDateTime != null && raceDateTime.isAfter(currentDateTime)) {
                    if (closestDateTime == null || raceDateTime.isBefore(closestDateTime)) {
                        closestDateTime = raceDateTime;
                        nextRace = race;
                    }
                }
            }

            if (nextRace != null) {
                Log.d(TAG, "Next weekly race found in local database");
                callback.onSuccess(nextRace);
            } else {
                Log.d(TAG, "No next weekly race found in local database");
                callback.onFailure(new Exception("No next weekly race found in local database"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving next weekly race from database: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    public void getLastRace(SingleWeeklyRaceCallback callback) {
        Log.d(TAG, "Fetching last weekly race from local database");
        try {
            // Get all races and filter for the last one (most recent past race)
            List<WeeklyRaceClassic> classicRaces = weeklyRaceClassicDao.getAllRaces();
            List<WeeklyRaceSprint> sprintRaces = weeklyRaceSprintDao.getAllRaces();

            WeeklyRace lastRace = null;
            LocalDateTime currentDateTime = LocalDateTime.now();
            LocalDateTime mostRecentDateTime = null;

            // Find the most recent classic race
            for (WeeklyRaceClassic race : classicRaces) {
                LocalDateTime raceDateTime = race.getDateTime();
                if (raceDateTime != null && raceDateTime.isBefore(currentDateTime)) {
                    if (mostRecentDateTime == null || raceDateTime.isAfter(mostRecentDateTime)) {
                        mostRecentDateTime = raceDateTime;
                        lastRace = race;
                    }
                }
            }

            // Find the most recent sprint race
            for (WeeklyRaceSprint race : sprintRaces) {
                LocalDateTime raceDateTime = race.getDateTime();
                if (raceDateTime != null && raceDateTime.isBefore(currentDateTime)) {
                    if (mostRecentDateTime == null || raceDateTime.isAfter(mostRecentDateTime)) {
                        mostRecentDateTime = raceDateTime;
                        lastRace = race;
                    }
                }
            }

            if (lastRace != null) {
                Log.d(TAG, "Last weekly race found in local database");
                callback.onSuccess(lastRace);
            } else {
                Log.d(TAG, "No last weekly race found in local database");
                callback.onFailure(new Exception("No last weekly race found in local database"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving last weekly race from database: " + e.getMessage());
            callback.onFailure(e);
        }
    }
}