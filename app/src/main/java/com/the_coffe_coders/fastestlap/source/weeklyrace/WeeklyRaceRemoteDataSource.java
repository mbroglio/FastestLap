package com.the_coffe_coders.fastestlap.source.weeklyrace;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeeklyRaceRemoteDataSource extends BaseWeeklyRaceRemoteDataSource{

    private final ErgastAPIService ergastAPIService;

    public WeeklyRaceRemoteDataSource(String apiKey) {
        this.ergastAPIService = ServiceLocator.getInstance().getWeeklyRaceAPIService();
    }


    @Override
    public void getWeeklyRacesList() {

    }

    @Override
    public void getLastWeeklyRace() {

    }

    @Override
    public void getNextWeeklyRace() {

    }
}
