package com.the_coffe_coders.fastestlap.util;

import android.app.Application;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.constructor.CommonConstructorRepository;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorStandingsRepository;
import com.the_coffe_coders.fastestlap.repository.driver.CommonDriverRepository;
import com.the_coffe_coders.fastestlap.repository.driver.DriverStandingsRepository;
import com.the_coffe_coders.fastestlap.repository.nation.FirebaseNationRepository;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.repository.user.UserRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorLocalDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.ConstructorLocalDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.ConstructorRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.driver.DriverLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver.DriverRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.result.RaceResultLocalDataSource;
import com.the_coffe_coders.fastestlap.source.result.RaceResultRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.user.BaseUserAuthenticationRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.user.BaseUserDataRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.user.UserAuthenticationFirebaseDataSource;
import com.the_coffe_coders.fastestlap.source.user.UserFirebaseDataSource;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceLocalDataSource;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.weeklyrace.WeeklyRaceLocalDataSource;
import com.the_coffe_coders.fastestlap.source.weeklyrace.WeeklyRaceRemoteDataSource;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ServiceLocator {

    public static ServiceLocator instance;
    public static String BASE_URL = "https://api.jolpi.ca/ergast/f1/";
    public String CURRENT_YEAR_BASE_URL = BASE_URL + "2024/";

    public static synchronized ServiceLocator getInstance() {
        if (instance == null) {
            instance = new ServiceLocator();
        }
        return instance;
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
            public Call<ResponseBody> getRaceResults(int round) {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getRaceResults(round);
            }

            @Override
            public Call<ResponseBody> getNextRace() {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(CURRENT_YEAR_BASE_URL)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                return retrofit.create(ErgastAPIService.class).getNextRace();
            }

            @Override
            public Call<ResponseBody> getCircuits() {
                return null;
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

    public DriverStandingsRepository getDriverStandingsRepository(Application application, boolean debugMode) {
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

        return new DriverStandingsRepository(driverRemoteDataSource, driverLocalDataSource);
    }

    public ConstructorStandingsRepository getConstructorStandingsRepository(Application application, boolean debugMode) {
        BaseConstructorRemoteDataSource constructorRemoteDataSource;
        BaseConstructorLocalDataSource constructorLocalDataSource;
        SharedPreferencesUtils sharedPreferencesUtil = new SharedPreferencesUtils(application);

        if (false) {//TODO change in debugMode
            /*JSONParserUtils jsonParserUtil = new JSONParserUtils(application);
            newsRemoteDataSource =
                    new DriverMockDataSource(jsonParserUtil);*/
        } else {
            constructorRemoteDataSource = new ConstructorRemoteDataSource("");
        }

        constructorLocalDataSource = new ConstructorLocalDataSource(getRoomDatabase(application), sharedPreferencesUtil);

        return new ConstructorStandingsRepository(constructorRemoteDataSource, constructorLocalDataSource);
    }

    public CommonDriverRepository getCommonDriverRepository(Application application, boolean b) {
        return new CommonDriverRepository();
    }

    public CommonConstructorRepository getCommonConstructorRepository(Application application, boolean b) {
        return new CommonConstructorRepository();
    }

    public FirebaseNationRepository getFirebaseNationRepository(Application application, boolean b) {
        return new FirebaseNationRepository();
    }

    public RaceRepository getRaceRepository(Application application, boolean b) {
        BaseWeeklyRaceLocalDataSource weeklyRaceLocalDataSource = new WeeklyRaceLocalDataSource(getRoomDatabase(application), new SharedPreferencesUtils(application));
        BaseWeeklyRaceRemoteDataSource weeklyRaceRemoteDataSource = new WeeklyRaceRemoteDataSource("");
        return new RaceRepository(weeklyRaceRemoteDataSource, weeklyRaceLocalDataSource, getRaceResultRepository(application, b));
    }

    public RaceResultRepository getRaceResultRepository(Application application, boolean b) {
        RaceResultRemoteDataSource raceResultRemoteDataSource = new RaceResultRemoteDataSource("");
        RaceResultLocalDataSource raceResultLocalDataSource = new RaceResultLocalDataSource(getRoomDatabase(application), new SharedPreferencesUtils(application));
        return new RaceResultRepository(raceResultRemoteDataSource, raceResultLocalDataSource);
    }

    public IUserRepository getUserRepository(Application application) {
        SharedPreferencesUtils sharedPreferencesUtil = new SharedPreferencesUtils(application);

        BaseUserAuthenticationRemoteDataSource userRemoteAuthenticationDataSource =
                new UserAuthenticationFirebaseDataSource();

        BaseUserDataRemoteDataSource userDataRemoteDataSource =
                new UserFirebaseDataSource(sharedPreferencesUtil);

        /*BaseArticleLocalDataSource newsLocalDataSource =
                new ArticleLocalDataSource(getNewsDao(application), sharedPreferencesUtil);*/

        return new UserRepository(userRemoteAuthenticationDataSource,
                userDataRemoteDataSource);
    }
}
