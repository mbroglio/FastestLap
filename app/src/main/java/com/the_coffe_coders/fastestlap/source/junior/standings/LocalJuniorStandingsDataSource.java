package com.the_coffe_coders.fastestlap.source.junior.standings;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.junior.JuniorStandingsDAO;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandings;
import com.the_coffe_coders.fastestlap.repository.junior.standings.JuniorStandingsCallback;

public class LocalJuniorStandingsDataSource implements JuniorStandingsDataSource {
    private static final String TAG = "LocalJuniorStandingsDataSource";
    private static LocalJuniorStandingsDataSource instance;
    private final JuniorStandingsDAO juniorStandingsDAO;

    public LocalJuniorStandingsDataSource(AppRoomDatabase appRoomDatabase) {
        this.juniorStandingsDAO = appRoomDatabase.juniorStandingsDAO();
    }

    public static synchronized LocalJuniorStandingsDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalJuniorStandingsDataSource(appRoomDatabase);
        }
        return instance;
    }


    @Override
    public void getJuniorDriverStandings(String series, JuniorStandingsCallback callback) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            Log.d(TAG, "Fetching junior driver standings from local database");
            JuniorDriverStandings result = juniorStandingsDAO.getDriverStandingsBySeries(series);

            if (result != null && result.getDriverStandings() != null && !result.getDriverStandings().isEmpty()) {
                callback.onDriverStandingsLoaded(result);
            } else {
                callback.onError(new Exception("No junior driver standings found in local database"));
            }
        });
    }

    @Override
    public void getJuniorConstructorStandings(String series, JuniorStandingsCallback callback) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            Log.d(TAG, "Fetching junior constructor standings from local database");
            JuniorConstructorStandings result = juniorStandingsDAO.getConstructorStandingsBySeries(series);

            if (result != null && result.getConstructorStandings() != null && !result.getConstructorStandings().isEmpty()) {
                callback.onConstructorStandingsLoaded(result);
            } else {
                callback.onError(new Exception("No junior constructor standings found in local database"));
            }
        });
    }

    public void insertJuniorDriverStandings(JuniorDriverStandings standings) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> juniorStandingsDAO.insertDriverStandings(standings));
    }

    public void insertJuniorConstructorStandings(JuniorConstructorStandings standings) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> juniorStandingsDAO.insertConstructorStandings(standings));
    }

    public void deleteJuniorDriverStandings(String series) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> juniorStandingsDAO.deleteDriverStandingsBySeries(series));
    }

    public void deleteJuniorConstructorStandings(String series) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> juniorStandingsDAO.deleteConstructorStandingsBySeries(series));
    }
}
