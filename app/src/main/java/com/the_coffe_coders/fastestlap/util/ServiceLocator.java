package com.the_coffe_coders.fastestlap.util;

import com.the_coffe_coders.fastestlap.service.ErgastAPIService;

import okhttp3.ResponseBody;
import retrofit2.Call;

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
                return null;
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
