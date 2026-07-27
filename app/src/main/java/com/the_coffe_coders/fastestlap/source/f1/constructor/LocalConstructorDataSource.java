package com.the_coffe_coders.fastestlap.source.f1.constructor;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.f1.ConstructorDAO;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.repository.f1.constructor.ConstructorCallback;

public class LocalConstructorDataSource implements ConstructorDataSource {

    private static final String TAG = "ConstructorLocalDataSource";
    private static LocalConstructorDataSource instance;
    private final ConstructorDAO constructorDAO;
    private final AppRoomDatabase appRoomDatabase;

    private LocalConstructorDataSource(AppRoomDatabase appRoomDatabase) {
        this.appRoomDatabase = appRoomDatabase;
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
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Constructor constructor = constructorDAO.getById(constructorId);
                // A valid cached constructor from Firebase must have rich details like car_pic_url, team_logo_url, or drivers list.
                // If it is missing all of these, it is an incomplete DTO object and should be treated as a cache miss.
                boolean isComplete = constructor != null &&
                        (constructor.getCar_pic_url() != null || constructor.getTeam_logo_url() != null ||
                         constructor.getTeam_logo_minimal_url() != null || (constructor.getDrivers() != null && !constructor.getDrivers().isEmpty()));

                if (isComplete) {
                    callback.onConstructorLoaded(constructor);
                } else {
                    callback.onConstructorLoaded(null);
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void insertConstructor(Constructor constructor) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> constructorDAO.insertConstructor(constructor));
    }
}
