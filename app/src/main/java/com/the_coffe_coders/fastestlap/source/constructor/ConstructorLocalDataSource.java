package com.the_coffe_coders.fastestlap.source.constructor;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.ConstructorStandingsDAO;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import java.util.List;

public class ConstructorLocalDataSource extends BaseConstructorLocalDataSource {
    private final ConstructorStandingsDAO constructorStandingsDAO;
    private final SharedPreferencesUtils sharedPreferencesUtil;

    public ConstructorLocalDataSource(AppRoomDatabase appRoomDatabase, SharedPreferencesUtils sharedPreferencesUtil) {
        this.constructorStandingsDAO = appRoomDatabase.constructorStandingsDao();
        this.sharedPreferencesUtil = sharedPreferencesUtil;
    }

    @Override
    public void getConstructorStandings() {
        constructorCallback.onSuccessFromLocal(constructorStandingsDAO.getConstructorStandings());
    }

    @Override
    public void insertConstructorsStandings(ConstructorStandings constructorStandings) {
        AppRoomDatabase.databaseWriteExecutor.execute(() -> {
            constructorStandingsDAO.insert(constructorStandings);
            if (constructorStandings != null) {
                sharedPreferencesUtil.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                        Constants.SHARED_PREFERENCES_LAST_UPDATE, String.valueOf(System.currentTimeMillis()));

                constructorCallback.onSuccessFromLocal(constructorStandings);
            }
        });
    }
}
