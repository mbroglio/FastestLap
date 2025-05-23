package com.the_coffe_coders.fastestlap.source.result;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.RaceDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultCallback;

import java.util.List;

public class LocalRaceResultDataSource implements RaceResultDataSource {
    private static final String TAG = "LocalRaceResultDataSource";
    private static LocalRaceResultDataSource instance;
    private final RaceDAO raceDAO;

    private LocalRaceResultDataSource(AppRoomDatabase appRoomDatabase) {
        this.raceDAO = appRoomDatabase.raceDAO();
    }

    public static synchronized LocalRaceResultDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalRaceResultDataSource(appRoomDatabase);
        }
        return instance;
    }

    public void getRaceResults(String round, RaceResultCallback callback) {
        Log.d(TAG, "Fetching race results from local database for round: " + round);
        try {
            int roundInt = Integer.parseInt(round);
            Race race = raceDAO.getRaceByRound(roundInt);
            if (race != null) {
                Log.d(TAG, "Race results found in local database for round: " + round);
                callback.onSuccess(race);
            } else {
                Log.d(TAG, "No race results found in local database for round: " + round);
                callback.onFailure(new Exception("No race results found in local database"));
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid round format: " + round);
            callback.onFailure(new Exception("Invalid round format: " + round));
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving race results from database: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    public void getRaceResults(int round, RaceResultCallback callback) {
        Log.d(TAG, "Fetching race results from local database for round: " + round);
        try {
            Race race = raceDAO.getRaceByRound(round);
            if (race != null) {
                Log.d(TAG, "Race results found in local database for round: " + round);
                callback.onSuccess(race);
            } else {
                Log.d(TAG, "No race results found in local database for round: " + round);
                callback.onFailure(new Exception("No race results found in local database"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving race results from database: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    public void insertRaceResults(Race race) {
        Log.d(TAG, "Inserting race results into local database for round: " + race.getRound());
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                raceDAO.insert(race);
                Log.d(TAG, "Race results successfully inserted into local database for round: " + race.getRound());
            } catch (Exception e) {
                Log.e(TAG, "Error inserting race results into database: " + e.getMessage());
            }
        });
    }

    public void insertAllRaceResults(List<Race> races) {
        Log.d(TAG, "Inserting multiple race results into local database");
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                raceDAO.insertAll(races);
                Log.d(TAG, "All race results successfully inserted into local database. Count: " + races.size());
            } catch (Exception e) {
                Log.e(TAG, "Error inserting multiple race results into database: " + e.getMessage());
            }
        });
    }

    public void getAllRaceResults(RaceResultCallback callback) {
        Log.d(TAG, "Fetching all race results from local database");
        try {
            List<Race> races = raceDAO.getAllRacesSortedByRound();
            if (races != null && !races.isEmpty()) {
                Log.d(TAG, "Found " + races.size() + " race results in local database");
                for (Race race : races) {
                    callback.onSuccess(race);
                }
            } else {
                Log.d(TAG, "No race results found in local database");
                callback.onFailure(new Exception("No race results found in local database"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving all race results from database: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    public void deleteAllRaceResults() {
        Log.d(TAG, "Deleting all race results from local database");
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                raceDAO.deleteAll();
                Log.d(TAG, "All race results successfully deleted from local database");
            } catch (Exception e) {
                Log.e(TAG, "Error deleting all race results from database: " + e.getMessage());
            }
        });
    }

    public void deleteRaceResult(Race race) {
        Log.d(TAG, "Deleting race result from local database for round: " + race.getRound());
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                raceDAO.delete(race);
                Log.d(TAG, "Race result successfully deleted from local database for round: " + race.getRound());
            } catch (Exception e) {
                Log.e(TAG, "Error deleting race result from database: " + e.getMessage());
            }
        });
    }
}