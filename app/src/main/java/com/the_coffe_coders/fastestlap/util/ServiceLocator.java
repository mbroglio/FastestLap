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

import java.io.File;
import java.util.Calendar;

import okhttp3.Cache;
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
    public static String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    public static String CURRENT_YEAR_BASE_URL = BASE_URL + currentYear + "/";
    private OkHttpClient sharedOkHttpClient;
    private ErgastAPIService ergastAPIService;
    private Application applicationContext;

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

    public synchronized void setApplicationContext(Application application) {
        this.applicationContext = application;
    }

    public synchronized OkHttpClient getOkHttpClient() {
        if (sharedOkHttpClient == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(new RetryInterceptor());

            if (applicationContext != null) {
                File cacheDir = new File(applicationContext.getCacheDir(), "http_cache");
                Cache cache = new Cache(cacheDir, 20 * 1024 * 1024); // 20 MB Cache
                builder.cache(cache);
            }

            sharedOkHttpClient = builder.build();
        }
        return sharedOkHttpClient;
    }

    public synchronized ErgastAPIService getConcreteErgastAPIService() {
        if (ergastAPIService == null) {
            OkHttpClient client = getOkHttpClient();

            Retrofit scalarRetrofit = new Retrofit.Builder()
                    .baseUrl(CURRENT_YEAR_BASE_URL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .client(client)
                    .build();

            Retrofit gsonRetrofit = new Retrofit.Builder()
                    .baseUrl(CURRENT_YEAR_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            ErgastAPIService scalarService = scalarRetrofit.create(ErgastAPIService.class);
            ErgastAPIService gsonService = gsonRetrofit.create(ErgastAPIService.class);

            ergastAPIService = new ErgastAPIService() {
                @Override
                public Call<ResponseBody> getConstructorStandings() {
                    return scalarService.getConstructorStandings();
                }

                @Override
                public Call<ResponseBody> getDriverStandings() {
                    return gsonService.getDriverStandings();
                }

                @Override
                public Call<ResponseBody> getRaces() {
                    return gsonService.getRaces();
                }

                @Override
                public Call<ResponseBody> getResults() {
                    return scalarService.getResults();
                }

                @Override
                public Call<ResponseBody> getLastRaceResults() {
                    return scalarService.getLastRaceResults();
                }

                @Override
                public Call<ResponseBody> getLastRace() {
                    return scalarService.getLastRace();
                }

                @Override
                public Call<ResponseBody> getRaceResults(int round) {
                    return scalarService.getRaceResults(round);
                }

                @Override
                public Call<ResponseBody> getQualifyingResults(int round) {
                    return scalarService.getQualifyingResults(round);
                }

                @Override
                public Call<ResponseBody> getSprintResults(int round) {
                    return scalarService.getSprintResults(round);
                }

                @Override
                public Call<ResponseBody> getNextRace() {
                    Log.i("Service Locator", "getNextRace: " + CURRENT_YEAR_BASE_URL);
                    return scalarService.getNextRace();
                }

                @Override
                public Call<ResponseBody> getDriver(String driverId) {
                    return scalarService.getDriver(driverId);
                }

                @Override
                public Call<ResponseBody> getDrivers() {
                    return scalarService.getDrivers();
                }

                @Override
                public Call<ResponseBody> getConstructor(String constructorId) {
                    return scalarService.getConstructor(constructorId);
                }

                @Override
                public Call<ResponseBody> getConstructors() {
                    return scalarService.getConstructors();
                }

                @Override
                public Call<ResponseBody> getJustFinishedRace(int round) {
                    return scalarService.getJustFinishedRace(round);
                }
            };
        }
        return ergastAPIService;
    }

    public AppRoomDatabase getRoomDatabase(Application application) {
        setApplicationContext(application);
        return AppRoomDatabase.getDatabase(application);
    }

    public IUserRepository getUserRepository(Application application) {
        setApplicationContext(application);
        SharedPreferencesUtils sharedPreferencesUtil = new SharedPreferencesUtils(application);

        BaseUserAuthenticationRemoteDataSource userRemoteAuthenticationDataSource =
                new UserAuthenticationFirebaseDataSource();

        BaseUserDataRemoteDataSource userDataRemoteDataSource =
                new UserFirebaseDataSource(sharedPreferencesUtil);

        return new UserRepository(userRemoteAuthenticationDataSource,
                userDataRemoteDataSource);
    }
}
