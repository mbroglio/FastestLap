package com.the_coffe_coders.fastestlap.source.nation;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.NationDAO;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.repository.nation.NationCallback;

public class LocalNationDataSource implements NationDataSource {
    private static LocalNationDataSource instance;
    private final NationDAO nationDAO;
    private static final String TAG = "NationLocalDataSource";

    private LocalNationDataSource(AppRoomDatabase appRoomDatabase) {
        this.nationDAO = appRoomDatabase.nationDAO();
    }

    public static synchronized LocalNationDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalNationDataSource(appRoomDatabase);
        }
        return instance;
    }

    @Override
    public void getNation(String nationId, NationCallback callback) {
        Log.d(TAG, "Fetching nation with ID: " + nationId);
        callback.onNationLoaded(nationDAO.getById(nationId));
    }

    public void insertNation(Nation nation) {
        nationDAO.insertNation(nation);
    }
}