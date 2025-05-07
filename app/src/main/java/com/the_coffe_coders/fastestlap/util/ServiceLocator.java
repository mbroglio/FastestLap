package com.the_coffe_coders.fastestlap.util;

import android.app.Application;
import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.repository.user.UserRepository;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.source.user.BaseUserAuthenticationRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.user.BaseUserDataRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.user.UserAuthenticationFirebaseDataSource;
import com.the_coffe_coders.fastestlap.source.user.UserFirebaseDataSource;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ServiceLocator {

    public static final String BASE_URL = "https://api.jolpi.ca/ergast/f1/";
    public static ServiceLocator instance;
    public static String currentYear = "2025";
    public static String CURRENT_YEAR_BASE_URL = BASE_URL + "${currentYear}" + "/";

    public static synchronized ServiceLocator getInstance() {
        if (instance == null) {
            instance = new ServiceLocator();
        }
        return instance;
    }

    public static void setCurrentYearBaseUrl(String seasonYear) {
        currentYear = seasonYear;
        CURRENT_YEAR_BASE_URL = BASE_URL + seasonYear + "/";
    }

    public ErgastAPIService getConcreteErgastAPIService() {
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
                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                // Build an OkHttpClient with the logging interceptor
                OkHttpClient okHttpClient = new OkHttpClient.Builder()
                        .addInterceptor(loggingInterceptor)
                        .build();

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(okHttpClient)
                        .build();

                return retrofit.create(ErgastAPIService.class).getDriverStandings();
            }

            @Override
            public Call<ResponseBody> getRaces() {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getRaces();
            }

            @Override
            public Call<ResponseBody> getResults() {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();
                return retrofit.create(ErgastAPIService.class).getResults();
            }

            @Override
            public Call<ResponseBody> getLastRaceResults() {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getLastRaceResults();
            }

            @Override
            public Call<ResponseBody> getLastRace() {

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getLastRace();
            }

            @Override
            public Call<ResponseBody> getRaceResults(int round) {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getRaceResults(round);
            }

            @Override
            public Call<ResponseBody> getNextRace() {
                Log.i("Service Locator", "getNextRace: " + CURRENT_YEAR_BASE_URL);

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getNextRace();
            }


            @Override
            public Call<ResponseBody> getDriver(String driverId) {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getDriver(driverId);
            }

            @Override
            public Call<ResponseBody> getDrivers() {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getDrivers();
            }

            @Override
            public Call<ResponseBody> getConstructor(String constructorId) {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getConstructor(constructorId);
            }
        };
    }

    public AppRoomDatabase getRoomDatabase(Application application) {
        return AppRoomDatabase.getDatabase(application);
    }

    public IUserRepository getUserRepository(Application application) {
        SharedPreferencesUtils sharedPreferencesUtil = new SharedPreferencesUtils(application);

        BaseUserAuthenticationRemoteDataSource userRemoteAuthenticationDataSource =
                new UserAuthenticationFirebaseDataSource();

        BaseUserDataRemoteDataSource userDataRemoteDataSource =
                new UserFirebaseDataSource(sharedPreferencesUtil);

        return new UserRepository(userRemoteAuthenticationDataSource,
                userDataRemoteDataSource);
    }
}
