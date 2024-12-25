package com.the_coffe_coders.fastestlap.util;

import static com.the_coffe_coders.fastestlap.repository.JolpicaServer.CURRENT_YEAR_BASE_URL;

import com.the_coffe_coders.fastestlap.service.ErgastAPIService;

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

    public ErgastAPIService getArticleAPIService() {
        return new ErgastAPIService() {
            @Override
            public Call<ResponseBody> getConstructorStandings() {
                return null;
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

}
