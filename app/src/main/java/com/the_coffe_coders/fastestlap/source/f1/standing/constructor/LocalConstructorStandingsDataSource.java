package com.the_coffe_coders.fastestlap.source.f1.standing.constructor;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.f1.ConstructorStandingsDAO;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;
import com.the_coffe_coders.fastestlap.repository.f1.standing.constructor.ConstructorStandingCallback;

import java.util.List;

public class LocalConstructorStandingsDataSource implements ConstructorStandingDataSource {
    private static final String TAG = "LocalConstructorStandingsDataSource";
    private static LocalConstructorStandingsDataSource instance;
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
        ConstructorStandings standings = constructorStandingsDAO.get();
        if (standings != null) {
            callback.onConstructorLoaded(standings);
        } else {
            callback.onError(new Exception("No constructor standings found in local database"));
        }
    }

    public void insertConstructorStandings(ConstructorStandings constructorStandings) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> constructorStandingsDAO.insert(constructorStandings));
    }

    public void insertConstructorList(List<Constructor> constructorList) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> constructorStandingsDAO.insertConstructorList(constructorList));
    }
}