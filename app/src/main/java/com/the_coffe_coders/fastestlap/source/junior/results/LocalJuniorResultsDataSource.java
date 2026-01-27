package com.the_coffe_coders.fastestlap.source.junior.results;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.junior.JuniorResultsDAO;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.repository.junior.result.JuniorResultCallback;

public class LocalJuniorResultsDataSource implements JuniorResultsDataSource {
    private static final String TAG = "LocalJuniorResultsDataSource";
    private static LocalJuniorResultsDataSource instance;
    private final JuniorResultsDAO juniorResultsDAO;

    public LocalJuniorResultsDataSource(AppRoomDatabase appRoomDatabase) {
        this.juniorResultsDAO = appRoomDatabase.juniorResultsDAO();
    }

    public static synchronized LocalJuniorResultsDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalJuniorResultsDataSource(appRoomDatabase);
        }
        return instance;
    }

    @Override
    public void getJuniorResults(String series, JuniorResultCallback callback) {
        Log.d(TAG, "Fetching junior results from local database");
        JuniorResult result = juniorResultsDAO.getBySeries(series);

        Log.i(TAG, "Result: " + result);

        if (result != null && result.getResults() != null && !result.getResults().isEmpty()) {
            callback.onResultLoaded(result);
        }else{
            callback.onError(new Exception("No junior results found in local database"));

        }
    }

    public void insertJuniorResult(JuniorResult result) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> juniorResultsDAO.insert(result));
    }

    public void deleteJuniorResult(String series) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> juniorResultsDAO.deleteBySeries(series));
    }
}
