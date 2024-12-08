package com.the_coffe_coders.fastestlap.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ResultsAPIResponse;

public class JSONParserUtils {
    Context context;

    public JSONParserUtils(Context context) {
        this.context = context;
    }

    public JSONParserUtils() {}

    public com.the_coffe_coders.fastestlap.domain.driver.StandingsAPIResponse parseDriverStandings(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, com.the_coffe_coders.fastestlap.domain.driver.StandingsAPIResponse.class);
    }

    public com.the_coffe_coders.fastestlap.domain.constructor.StandingsAPIResponse parseConstructorStandings(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, com.the_coffe_coders.fastestlap.domain.constructor.StandingsAPIResponse.class);
    }

    public RaceAPIResponse parseRace(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, RaceAPIResponse.class);
    }

    public ResultsAPIResponse parseRaceResults(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, ResultsAPIResponse.class);
    }
}
