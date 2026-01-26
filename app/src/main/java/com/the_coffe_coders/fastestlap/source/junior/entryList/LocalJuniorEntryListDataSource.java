package com.the_coffe_coders.fastestlap.source.junior.entryList;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.junior.JuniorEntryListDAO;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorEntryList;
import com.the_coffe_coders.fastestlap.repository.junior.entrylist.JuniorEntryListCallback;

public class LocalJuniorEntryListDataSource implements JuniorEntryListDataSource{
    private static final String TAG = "LocalJuniorEntryListDataSource";
    private static LocalJuniorEntryListDataSource instance;
    private final JuniorEntryListDAO juniorEntryListDAO;

    public LocalJuniorEntryListDataSource(AppRoomDatabase appRoomDatabase){
        this.juniorEntryListDAO = appRoomDatabase.juniorEntryListDAO();
    }

    public static synchronized LocalJuniorEntryListDataSource getInstance(AppRoomDatabase appRoomDatabase){
        if(instance == null){
            instance = new LocalJuniorEntryListDataSource(appRoomDatabase);
        }
        return instance;
    }


    @Override
    public void getJuniorEntryList(String series, JuniorEntryListCallback callback) {
        Log.d(TAG, "Fetching junior entry list from local database");
        JuniorEntryList entryList = juniorEntryListDAO.getBySeries(series);
        if(entryList != null){
            callback.onEntryListLoaded(entryList);
        }else{
            callback.onError(new Exception("No junior entry list found in local database"));
        }
    }

    public void insertJuniorEntryList(JuniorEntryList entryList){
        AppRoomDatabase.databaseWriteExecutor.execute(() -> juniorEntryListDAO.insert(entryList));
    }
}
