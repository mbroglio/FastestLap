package com.the_coffe_coders.fastestlap.source.f1.result;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.f1.QualifyingDAO;
import com.the_coffe_coders.fastestlap.database.f1.RaceDAO;
import com.the_coffe_coders.fastestlap.database.f1.SprintDAO;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.repository.f1.result.RaceResultCallback;

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
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
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
        });
    }

    public void getRaceResults(int round, RaceResultCallback callback) {
        Log.d(TAG, "Fetching race results from local database for round: " + round);
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
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
        });
    }

    public void insertRaceResults(Race race) {
        insertRaceResults(race, null);
    }

    public void insertRaceResults(Race race, Runnable onMergedAndInserted) {
        Log.d(TAG, "Inserting race results into local database for round: " + (race != null ? race.getRound() : "null"));
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (race != null && race.getRound() != null) {
                    try {
                        int roundInt = Integer.parseInt(race.getRound());
                        Race existing = raceDAO.getRaceByRound(roundInt);
                        if (existing != null) {
                            boolean restored = false;
                            if ((race.getRaceStints() == null || race.getRaceStints().isEmpty())
                                    && existing.getRaceStints() != null && !existing.getRaceStints().isEmpty()) {
                                race.setRaceStints(existing.getRaceStints());
                                restored = true;
                            }
                            if ((race.getSprintStints() == null || race.getSprintStints().isEmpty())
                                    && existing.getSprintStints() != null && !existing.getSprintStints().isEmpty()) {
                                race.setSprintStints(existing.getSprintStints());
                                restored = true;
                            }
                            if (restored) {
                                Log.d(TAG, "Preserved existing stints from local database for race round: " + race.getRound());
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                raceDAO.insert(race);
                Log.d(TAG, "Race results successfully inserted into local database for round: " + (race != null ? race.getRound() : "null"));
                if (onMergedAndInserted != null) {
                    onMergedAndInserted.run();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting race results into database: " + e.getMessage());
            }
        });
    }

    public void getQualifyingResults(String round, RaceResultCallback callback) {
        Log.d(TAG, "Fetching qualifying results from local database for round: " + round);
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                int roundInt = Integer.parseInt(round);
                Race race = qualifyingDAO.getRaceByRound(roundInt);
                if (race != null) {
                    Log.d(TAG, "Qualifying results found in local database for round: " + round);
                    callback.onSuccess(race);
                } else {
                    Log.d(TAG, "No qualifying results found in local database for round: " + round);
                    callback.onFailure(new Exception("No qualifying results found in local database"));
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid round format: " + round);
                callback.onFailure(new Exception("Invalid round format: " + round));
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving qualifying results from database: " + e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void getQualifyingResults(int round, RaceResultCallback callback) {
        Log.d(TAG, "Fetching qualifying results from local database for round: " + round);
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Race race = qualifyingDAO.getRaceByRound(round);
                if (race != null) {
                    Log.d(TAG, "Qualifying results found in local database for round: " + round);
                    callback.onSuccess(race);
                } else {
                    Log.d(TAG, "No qualifying results found in local database for round: " + round);
                    callback.onFailure(new Exception("No qualifying results found in local database"));
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid round format: " + round);
                callback.onFailure(new Exception("Invalid round format: " + round));
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving qualifying results from database: " + e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void insertQualifyingResults(Race race) {
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

    public void getSprintResults(String round, RaceResultCallback callback) {
        Log.d(TAG, "Fetching sprint results from local database for round: " + round);
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                int roundInt = Integer.parseInt(round);
                Race race = sprintDAO.getRaceByRound(roundInt);
                if (race != null) {
                    Log.d(TAG, "sprint results found in local database for round: " + round);
                    callback.onSuccess(race);
                } else {
                    Log.d(TAG, "No sprint results found in local database for round: " + round);
                    callback.onFailure(new Exception("No sprint results found in local database"));
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid round format: " + round);
                callback.onFailure(new Exception("Invalid round format: " + round));
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving sprint results from database: " + e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void getSprintResults(int round, RaceResultCallback callback) {
        Log.d(TAG, "Fetching sprint results from local database for round: " + round);
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Race race = sprintDAO.getRaceByRound(round);
                if (race != null) {
                    Log.d(TAG, "sprint results found in local database for round: " + round);
                    callback.onSuccess(race);
                } else {
                    Log.d(TAG, "No sprint results found in local database for round: " + round);
                    callback.onFailure(new Exception("No sprint results found in local database"));
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid round format: " + round);
                callback.onFailure(new Exception("Invalid round format: " + round));
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving sprint results from database: " + e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void insertSprintResults(Race race) {
        insertSprintResults(race, null);
    }

    public void insertSprintResults(Race race, Runnable onMergedAndInserted) {
        Log.d(TAG, "Inserting sprint results into local database for round: " + (race != null ? race.getRound() : "null"));
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (race != null && race.getRound() != null) {
                    try {
                        int roundInt = Integer.parseInt(race.getRound());
                        Race existing = sprintDAO.getRaceByRound(roundInt);
                        if (existing != null) {
                            boolean restored = false;
                            if ((race.getSprintStints() == null || race.getSprintStints().isEmpty())
                                    && existing.getSprintStints() != null && !existing.getSprintStints().isEmpty()) {
                                race.setSprintStints(existing.getSprintStints());
                                restored = true;
                            }
                            if ((race.getRaceStints() == null || race.getRaceStints().isEmpty())
                                    && existing.getRaceStints() != null && !existing.getRaceStints().isEmpty()) {
                                race.setRaceStints(existing.getRaceStints());
                                restored = true;
                            }
                            if (restored) {
                                Log.d(TAG, "Preserved existing stints from local database for sprint round: " + race.getRound());
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                sprintDAO.insert(race);
                Log.d(TAG, "Sprint results successfully inserted into local database for round: " + (race != null ? race.getRound() : "null"));
                if (onMergedAndInserted != null) {
                    onMergedAndInserted.run();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting sprint results into database: " + e.getMessage());
            }
        });
    }


}