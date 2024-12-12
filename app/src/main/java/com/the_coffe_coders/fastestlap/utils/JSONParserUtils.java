package com.the_coffe_coders.fastestlap.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.driver.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Circuit;
import com.the_coffe_coders.fastestlap.domain.grand_prix.CircuitAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ResultsAPIResponse;

public class JSONParserUtils {
    Context context;

    public JSONParserUtils(Context context) {
        this.context = context;
    }

    public JSONParserUtils() {}

    public DriverStandingsAPIResponse parseDriverStandings(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, DriverStandingsAPIResponse.class);
    }

    public ConstructorStandingsAPIResponse parseConstructorStandings(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, ConstructorStandingsAPIResponse.class);
    }

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
