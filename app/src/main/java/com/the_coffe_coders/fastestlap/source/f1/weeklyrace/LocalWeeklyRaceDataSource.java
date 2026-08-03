package com.the_coffe_coders.fastestlap.source.f1.weeklyrace;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.f1.WeeklyRaceClassicDAO;
import com.the_coffe_coders.fastestlap.database.f1.WeeklyRaceSprintDAO;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRaceClassic;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRaceSprint;
import com.the_coffe_coders.fastestlap.repository.f1.weeklyrace.SingleWeeklyRaceCallback;
import com.the_coffe_coders.fastestlap.repository.f1.weeklyrace.WeeklyRacesCallback;

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
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<WeeklyRace> rawList = new ArrayList<>();
                rawList.addAll(weeklyRaceClassicDao.getAllRaces());
                rawList.addAll(weeklyRaceSprintDao.getAllRaces());

                List<WeeklyRace> weeklyRaceList = new ArrayList<>();
                java.util.Set<String> seenRounds = new java.util.HashSet<>();
                for (WeeklyRace race : rawList) {
                    if (race != null && race.getRound() != null) {
                        if (!seenRounds.contains(race.getRound())) {
                            seenRounds.add(race.getRound());
                            weeklyRaceList.add(race);
                        }
                    }
                }

                if (!weeklyRaceList.isEmpty()) {
                    Log.d(TAG, "Found " + weeklyRaceList.size() + " unique weekly races in local database");
                    callback.onSuccess(weeklyRaceList);
                } else {
                    Log.d(TAG, "No weekly races found in local database");
                    callback.onFailure(new Exception("No weekly races found in local database"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving weekly races from database: " + e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void saveWeeklyRaces(List<WeeklyRace> weeklyRaces) {
        Log.d(TAG, "Saving weekly races to local database. Count: " + weeklyRaces.size());
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
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
        });
    }

    public void saveSingleWeeklyRace(WeeklyRace weeklyRace) {
        if (weeklyRace == null || weeklyRace.getRound() == null) return;
        Log.d(TAG, "Saving single weekly race to local database: " + weeklyRace.getRound());
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                weeklyRaceClassicDao.delete(weeklyRace.getRound());
                weeklyRaceSprintDao.delete(weeklyRace.getRound());
                if (weeklyRace instanceof WeeklyRaceClassic) {
                    weeklyRaceClassicDao.insert((WeeklyRaceClassic) weeklyRace);
                } else if (weeklyRace instanceof WeeklyRaceSprint) {
                    weeklyRaceSprintDao.insert((WeeklyRaceSprint) weeklyRace);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving single weekly race to database: " + e.getMessage());
            }
        });
    }

    public void getNextRace(SingleWeeklyRaceCallback callback) {
        Log.d(TAG, "Fetching next weekly race from local database");
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
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
        });
    }

    public void getLastRace(SingleWeeklyRaceCallback callback) {
        Log.d(TAG, "Fetching last weekly race from local database");
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
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
        });
    }
}