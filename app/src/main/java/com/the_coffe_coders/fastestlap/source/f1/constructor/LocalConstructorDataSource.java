package com.the_coffe_coders.fastestlap.source.f1.constructor;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.f1.ConstructorDAO;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.repository.f1.constructor.ConstructorCallback;

public class LocalConstructorDataSource implements ConstructorDataSource {

    private static final String TAG = "ConstructorLocalDataSource";
    private static LocalConstructorDataSource instance;
    private final ConstructorDAO constructorDAO;

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
        try {
            Constructor constructor = constructorDAO.getById(constructorId);
            callback.onConstructorLoaded(constructor);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public void insertConstructor(Constructor constructor) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> constructorDAO.insertConstructor(constructor));
    }
}
