package com.the_coffe_coders.fastestlap.source.standing.constructor;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.ConstructorStandingsDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.repository.standing.constructor.ConstructorStandingCallback;

public class LocalConstructorStandingsDataSource implements ConstructorStandingDataSource {
    private static LocalConstructorStandingsDataSource instance;
    private static final String TAG = "LocalConstructorStandingsDataSource";
    private final ConstructorStandingsDAO constructorStandingsDAO;

    private LocalConstructorStandingsDataSource(AppRoomDatabase appRoomDatabase) {
        this.constructorStandingsDAO = appRoomDatabase.constructorStandingsDao();
    }

    public static synchronized LocalConstructorStandingsDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalConstructorStandingsDataSource(appRoomDatabase);
        }
        return instance;
    }

    @Override
    public void getConstructorStandings(ConstructorStandingCallback callback) {
        Log.d(TAG, "Fetching constructor standings from local database");
        ConstructorStandings standings = (ConstructorStandings) constructorStandingsDAO.get();
        if (standings != null) {
            callback.onConstructorLoaded(standings);
        } else {
            callback.onError(new Exception("No constructor standings found in local database"));
        }
    }

    public void insertConstructorStandings(ConstructorStandings constructorStandings) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            constructorStandingsDAO.insert(constructorStandings);
        });
    }
}