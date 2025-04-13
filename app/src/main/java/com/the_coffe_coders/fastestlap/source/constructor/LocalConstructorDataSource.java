package com.the_coffe_coders.fastestlap.source.constructor;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.ConstructorDAO;
import com.the_coffe_coders.fastestlap.database.ConstructorStandingsDAO;
import com.the_coffe_coders.fastestlap.database.DriverDAO;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorCallback;
import com.the_coffe_coders.fastestlap.source.driver.LocalDriverDataSource;

public class LocalConstructorDataSource implements ConstructorDataSource{

    private static LocalConstructorDataSource instance;
    private final ConstructorDAO constructorDAO;
    private static final String TAG = "ConstructorLocalDataSource";

    private LocalConstructorDataSource(AppRoomDatabase appRoomDatabase) {
        this.constructorDAO = appRoomDatabase.constructorDAO();
    }

    public static synchronized LocalConstructorDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalConstructorDataSource(appRoomDatabase);
        }
        return instance;

    }


    @Override
    public void getConstructor(String constructorId, ConstructorCallback callback) {


    }







}
