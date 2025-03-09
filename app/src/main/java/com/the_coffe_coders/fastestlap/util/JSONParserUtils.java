package com.the_coffe_coders.fastestlap.util;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ConstructorAPIResponse;
import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.DriversAPIResponse;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.api.RaceResultsAPIResponse;

public class JSONParserUtils {
    Context context;

    public JSONParserUtils(Context context) {
        this.context = context;
    }

    public JSONParserUtils() {
    }

    public DriverStandingsAPIResponse parseDriverStandings(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, DriverStandingsAPIResponse.class);
    }

    public ConstructorStandingsAPIResponse parseConstructorStandings(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, ConstructorStandingsAPIResponse.class);
    }

    public RaceAPIResponse parseRace(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, RaceAPIResponse.class);
    }

    public RaceResultsAPIResponse parseRaceResults(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, RaceResultsAPIResponse.class);
    }

    public DriversAPIResponse parseDrivers(JsonObject mrdata) {
        return new Gson().fromJson(mrdata, DriversAPIResponse.class);
    }

    public ConstructorAPIResponse parseConstructor(JsonObject mrdata) {
        return new Gson().fromJson(mrdata, ConstructorAPIResponse.class);
    }



    /*public CircuitAPIResponse parseCircuit(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, CircuitAPIResponse.class);
    }*/
}
