package com.the_coffe_coders.fastestlap.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.CircuitAPIResponse;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.ResultsAPIResponse;

public class JSONParserUtils {
    Context context;

    public JSONParserUtils(Context context) {
        this.context = context;
    }
    public JSONParserUtils() {}

    public DriverStandingsAPIResponse parseDriverStandings(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, DriverStandingsAPIResponse.class);
    }

    /*public ConstructorStandingsAPIResponse parseConstructorStandings(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, ConstructorStandingsAPIResponse.class);
    }*/

    public RaceAPIResponse parseRace(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, RaceAPIResponse.class);
    }

    public ResultsAPIResponse parseRaceResults(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, ResultsAPIResponse.class);
    }

    public CircuitAPIResponse parseCircuit(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, CircuitAPIResponse.class);
    }
}
