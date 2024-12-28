package com.the_coffe_coders.fastestlap.util;

import static com.the_coffe_coders.fastestlap.repository.JolpicaServer.CURRENT_YEAR_BASE_URL;

import android.app.Application;

import com.the_coffe_coders.fastestlap.database.DriverRoomDatabase;
import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorRepository;
import com.the_coffe_coders.fastestlap.repository.driver.DriverRepository;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorLocalDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.ConstructorLocalDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.ConstructorRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.driver.DriverLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver.DriverRemoteDataSource;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ServiceLocator {

    public static ServiceLocator instance;

    public static synchronized ServiceLocator getInstance() {
        if (instance == null) {
            instance = new ServiceLocator();
        }
        return instance;
    }

    public ErgastAPIService getConstructorAPIService() {
        return new ErgastAPIService() {
            @Override
            public Call<ResponseBody> getConstructorStandings() {

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getConstructorStandings();
            }

            @Override
            public Call<ResponseBody> getDriverStandings() {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getDriverStandings();
            }

            @Override
            public Call<ResponseBody> getRaces() {
                return null;
            }

            @Override
            public Call<ResponseBody> getLastRaceResults() {
                return null;
            }

            @Override
            public Call<ResponseBody> getResults() {
                return null;
            }

            @Override
            public Call<ResponseBody> getNextRace() {
                return null;
            }

            @Override
            public Call<ResponseBody> getCircuits() {
                return null;
            }
        };
    }

    public ErgastAPIService getDriverAPIService() {
        return new ErgastAPIService() {
            @Override
            public Call<ResponseBody> getConstructorStandings() {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getConstructorStandings();
            }

            @Override
            public Call<ResponseBody> getDriverStandings() {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getDriverStandings();
            }

            @Override
            public Call<ResponseBody> getRaces() {
                return null;
            }

            @Override
            public Call<ResponseBody> getLastRaceResults() {
                return null;
            }

            @Override
            public Call<ResponseBody> getResults() {
                return null;
            }

            @Override
            public Call<ResponseBody> getNextRace() {
                return null;
            }

            @Override
            public Call<ResponseBody> getCircuits() {
                return null;
            }
        };
    }

    public AppRoomDatabase getRoomDatabase(Application application) {
        return AppRoomDatabase.getDatabase(application);
    }

    public DriverRepository getDriverRepository(Application application, boolean debugMode) {
        BaseDriverRemoteDataSource driverRemoteDataSource;
        BaseDriverLocalDataSource driverLocalDataSource;
        SharedPreferencesUtils sharedPreferencesUtil = new SharedPreferencesUtils(application);

        if (false) {//TODO change in debugMode
            /*JSONParserUtils jsonParserUtil = new JSONParserUtils(application);
            newsRemoteDataSource =
                    new DriverMockDataSource(jsonParserUtil);*/
        } else {
            driverRemoteDataSource = new DriverRemoteDataSource("");
        }

        driverLocalDataSource = new DriverLocalDataSource(getRoomDatabase(application), sharedPreferencesUtil);

        return new DriverRepository(driverRemoteDataSource, driverLocalDataSource);
    }

    public ConstructorRepository getConstructorRepository(Application application, boolean debugMode) {
        BaseConstructorRemoteDataSource constructorRemoteDataSource;
        BaseConstructorLocalDataSource constructorLocalDataSource;

        if (false) {//TODO change in debugMode
            /*JSONParserUtils jsonParserUtil = new JSONParserUtils(application);
            newsRemoteDataSource =
                    new DriverMockDataSource(jsonParserUtil);*/
        } else {
            constructorRemoteDataSource = new ConstructorRemoteDataSource("");
    }

        constructorLocalDataSource = new ConstructorLocalDataSource(getRoomDatabase(application));

        return new ConstructorRepository(constructorRemoteDataSource, constructorLocalDataSource);
    }

}
