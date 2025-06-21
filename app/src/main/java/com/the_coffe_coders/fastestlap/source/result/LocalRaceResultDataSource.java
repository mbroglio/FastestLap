package com.the_coffe_coders.fastestlap.source.result;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.QualifyingDAO;
import com.the_coffe_coders.fastestlap.database.RaceDAO;
import com.the_coffe_coders.fastestlap.database.SprintDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultCallback;

public class LocalRaceResultDataSource implements RaceResultDataSource {
    private static final String TAG = "LocalRaceResultDataSource";
    private static LocalRaceResultDataSource instance;
    private final RaceDAO raceDAO;
    private final QualifyingDAO qualifyingDAO;
    private final SprintDAO sprintDAO;


    private LocalRaceResultDataSource(AppRoomDatabase appRoomDatabase) {
        this.raceDAO = appRoomDatabase.raceDAO();
        this.qualifyingDAO = appRoomDatabase.qualifyingDAO();
        this.sprintDAO = appRoomDatabase.sprintDAO();
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

    public void getQualifyingResults(String round, RaceResultCallback callback){
        Log.d(TAG, "Fetching qualifying results from local database for round: " + round);
        try {
            int roundInt = Integer.parseInt(round);
            Race race = qualifyingDAO.getRaceByRound(roundInt);
            if(race != null){
                Log.d(TAG, "Qualifying results found in local database for round: " + round);
                callback.onSuccess(race);
            }else{
                Log.d(TAG, "No qualifying results found in local database for round: " + round);
                callback.onFailure(new Exception("No qualifying results found in local database"));
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid round format: " + round);
            callback.onFailure(new Exception("Invalid round format: " + round));
        }catch(Exception e){
            Log.e(TAG, "Error retrieving qualifying results from database: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    public void getQualifyingResults(int round, RaceResultCallback callback){
        Log.d(TAG, "Fetching qualifying results from local database for round: " + round);
        try {
            Race race = qualifyingDAO.getRaceByRound(round);
            if(race != null){
                Log.d(TAG, "Qualifying results found in local database for round: " + round);
                callback.onSuccess(race);
            }else{
                Log.d(TAG, "No qualifying results found in local database for round: " + round);
                callback.onFailure(new Exception("No qualifying results found in local database"));
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid round format: " + round);
            callback.onFailure(new Exception("Invalid round format: " + round));
        }catch(Exception e){
            Log.e(TAG, "Error retrieving qualifying results from database: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    public void insertQualifyingResults(Race race){
        Log.d(TAG, "Inserting qualifying results into local database for round: " + race.getRound());
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                qualifyingDAO.insert(race);
                Log.d(TAG, "Qualifying results successfully inserted into local database for round: + race.getRound()");
            } catch (Exception e) {
                Log.e(TAG, "Error inserting qualifying results into database: " + e.getMessage());
            }
        });
    }

    public void getSprintResults(String round, RaceResultCallback callback){
        Log.d(TAG, "Fetching sprint results from local database for round: " + round);
        try {
            int roundInt = Integer.parseInt(round);
            Race race = sprintDAO.getRaceByRound(roundInt);
            if(race != null){
                Log.d(TAG, "sprint results found in local database for round: " + round);
                callback.onSuccess(race);
            }else{
                Log.d(TAG, "No sprint results found in local database for round: " + round);
                callback.onFailure(new Exception("No sprint results found in local database"));
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid round format: " + round);
            callback.onFailure(new Exception("Invalid round format: " + round));
        }catch(Exception e){
            Log.e(TAG, "Error retrieving sprint results from database: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    public void getSprintResults(int round, RaceResultCallback callback){
        Log.d(TAG, "Fetching sprint results from local database for round: " + round);
        try {
            Race race = sprintDAO.getRaceByRound(round);
            if(race != null){
                Log.d(TAG, "sprint results found in local database for round: " + round);
                callback.onSuccess(race);
            }else{
                Log.d(TAG, "No sprint results found in local database for round: " + round);
                callback.onFailure(new Exception("No sprint results found in local database"));
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid round format: " + round);
            callback.onFailure(new Exception("Invalid round format: " + round));
        }catch(Exception e){
            Log.e(TAG, "Error retrieving sprint results from database: " + e.getMessage());
            callback.onFailure(e);
        }
    }

    public void insertSprintResults(Race race){
        Log.d(TAG, "Inserting sprint results into local database for round: " + race.getRound());
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                sprintDAO.insert(race);
                Log.d(TAG, "sprint results successfully inserted into local database for round: + race.getRound()");
            } catch (Exception e) {
                Log.e(TAG, "Error inserting sprint results into database: " + e.getMessage());
            }
        });
    }




    
}