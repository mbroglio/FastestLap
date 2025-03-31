package com.the_coffe_coders.fastestlap.util;

import android.app.Application;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.nation.NationRepository;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.standings.constructor.ConstructorStandingsStandingsRepository;
import com.the_coffe_coders.fastestlap.repository.standings.driver.DriverStandingsRepository;
import com.the_coffe_coders.fastestlap.repository.track.TrackRepository;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.repository.user.UserRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.source.constructor_standings.BaseConstructorStandingsLocalDataSource;
import com.the_coffe_coders.fastestlap.source.constructor_standings.BaseConstructorStandingsRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.constructor_standings.ConstructorStandingsRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.constructor_standings.ConstructorStandingsStandingsLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver_standings.BaseDriverStandingsLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver_standings.BaseDriverStandingsRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.driver_standings.DriverStandingsLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver_standings.DriverStandingsRemoteDataSource;
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

    public static final String BASE_URL = "https://api.jolpi.ca/ergast/f1/";
    public static ServiceLocator instance;
    public static String currentYear = "2025";
    public static String CURRENT_YEAR_BASE_URL = BASE_URL + "${currentYear}";

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

    public DriverStandingsRepository getDriverStandingsRepository(Application application, boolean debugMode) {
        BaseDriverStandingsRemoteDataSource driverRemoteDataSource;
        BaseDriverStandingsLocalDataSource driverLocalDataSource;
        SharedPreferencesUtils sharedPreferencesUtil = new SharedPreferencesUtils(application);
        driverRemoteDataSource = new DriverStandingsRemoteDataSource();
        driverLocalDataSource = new DriverStandingsLocalDataSource(getRoomDatabase(application), sharedPreferencesUtil);
        return new DriverStandingsRepository(driverRemoteDataSource, driverLocalDataSource);
    }

    public ConstructorStandingsStandingsRepository getConstructorStandingsRepository(Application application, boolean debugMode) {
        BaseConstructorStandingsRemoteDataSource constructorRemoteDataSource;
        BaseConstructorStandingsLocalDataSource constructorLocalDataSource;
        SharedPreferencesUtils sharedPreferencesUtil = new SharedPreferencesUtils(application);
        constructorRemoteDataSource = new ConstructorStandingsRemoteDataSource("");
        constructorLocalDataSource = new ConstructorStandingsStandingsLocalDataSource(getRoomDatabase(application), sharedPreferencesUtil);
        return new ConstructorStandingsStandingsRepository(constructorRemoteDataSource, constructorLocalDataSource);
    }

    public NationRepository getFirebaseNationRepository() {
        return new NationRepository();
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

        return new UserRepository(userRemoteAuthenticationDataSource,
                userDataRemoteDataSource);
    }

    public TrackRepository getTrackRepository() {
        return new TrackRepository();
    }
}
