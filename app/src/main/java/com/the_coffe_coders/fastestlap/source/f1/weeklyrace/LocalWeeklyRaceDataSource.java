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
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LocalWeeklyRaceDataSource {
    private static final String TAG = "LocalWeeklyRaceDataSource";
    private static LocalWeeklyRaceDataSource instance;
    private final AppRoomDatabase appRoomDatabase;
    private final WeeklyRaceClassicDAO weeklyRaceClassicDao;
    private final WeeklyRaceSprintDAO weeklyRaceSprintDao;

    private LocalWeeklyRaceDataSource(AppRoomDatabase appRoomDatabase) {
        this.appRoomDatabase = appRoomDatabase;
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
                appRoomDatabase.runInTransaction(() -> {
                    weeklyRaceClassicDao.deleteAll();
                    weeklyRaceSprintDao.deleteAll();
                    for (WeeklyRace weeklyRace : weeklyRaces) {
                        if (weeklyRace instanceof WeeklyRaceClassic) {
                            weeklyRaceClassicDao.insert((WeeklyRaceClassic) weeklyRace);
                        } else if (weeklyRace instanceof WeeklyRaceSprint) {
                            weeklyRaceSprintDao.insert((WeeklyRaceSprint) weeklyRace);
                        }
                    }
                });
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
                appRoomDatabase.runInTransaction(() -> {
                    weeklyRaceClassicDao.delete(weeklyRace.getRound());
                    weeklyRaceSprintDao.delete(weeklyRace.getRound());
                    if (weeklyRace instanceof WeeklyRaceClassic) {
                        weeklyRaceClassicDao.insert((WeeklyRaceClassic) weeklyRace);
                    } else if (weeklyRace instanceof WeeklyRaceSprint) {
                        weeklyRaceSprintDao.insert((WeeklyRaceSprint) weeklyRace);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving single weekly race to database: " + e.getMessage());
            }
        });
    }

    public void getNextRace(SingleWeeklyRaceCallback callback) {
        Log.d(TAG, "Fetching next weekly race from local database");
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<WeeklyRaceClassic> classicRaces = weeklyRaceClassicDao.getAllRaces();
                List<WeeklyRaceSprint> sprintRaces = weeklyRaceSprintDao.getAllRaces();

                List<WeeklyRace> rawList = new ArrayList<>();
                rawList.addAll(classicRaces);
                rawList.addAll(sprintRaces);

                List<WeeklyRace> allRaces = new ArrayList<>();
                Set<String> seenRounds = new java.util.HashSet<>();
                for (WeeklyRace race : rawList) {
                    if (race != null && race.getRound() != null && !seenRounds.contains(race.getRound())) {
                        seenRounds.add(race.getRound());
                        allRaces.add(race);
                    }
                }

                Collections.sort(allRaces, (r1, r2) -> {
                    try {
                        return Integer.compare(Integer.parseInt(r1.getRound()), Integer.parseInt(r2.getRound()));
                    } catch (Exception e) {
                        return 0;
                    }
                });

                WeeklyRace nextRace = null;
                for (WeeklyRace race : allRaces) {
                    if (!race.isWeekFinished()) {
                        nextRace = race;
                        break;
                    }
                }

                if (nextRace != null) {
                    Log.d(TAG, "Next weekly race found in local database: Round " + nextRace.getRound());
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
                List<WeeklyRaceClassic> classicRaces = weeklyRaceClassicDao.getAllRaces();
                List<WeeklyRaceSprint> sprintRaces = weeklyRaceSprintDao.getAllRaces();

                List<WeeklyRace> rawList = new ArrayList<>();
                rawList.addAll(classicRaces);
                rawList.addAll(sprintRaces);

                List<WeeklyRace> allRaces = new ArrayList<>();
                Set<String> seenRounds = new java.util.HashSet<>();
                for (WeeklyRace race : rawList) {
                    if (race != null && race.getRound() != null && !seenRounds.contains(race.getRound())) {
                        seenRounds.add(race.getRound());
                        allRaces.add(race);
                    }
                }

                Collections.sort(allRaces, (r1, r2) -> {
                    try {
                        return Integer.compare(Integer.parseInt(r1.getRound()), Integer.parseInt(r2.getRound()));
                    } catch (Exception e) {
                        return 0;
                    }
                });

                WeeklyRace lastRace = null;
                for (int i = allRaces.size() - 1; i >= 0; i--) {
                    WeeklyRace race = allRaces.get(i);
                    if (race.isWeekFinished()) {
                        lastRace = race;
                        break;
                    }
                }

                if (lastRace != null) {
                    Log.d(TAG, "Last weekly race found in local database: Round " + lastRace.getRound());
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