package com.the_coffe_coders.fastestlap.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.domain.driver.StandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race_result.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race_week.RaceWeekAPIresponse;

public class JSONParserUtils {
    Context context;

    public JSONParserUtils(Context context) {
        this.context = context;
    }
    public com.the_coffe_coders.fastestlap.domain.driver.StandingsAPIResponse parseDriverStandings(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, com.the_coffe_coders.fastestlap.domain.driver.StandingsAPIResponse.class);
    }

    public com.the_coffe_coders.fastestlap.domain.constructor.StandingsAPIResponse parseConstructorStandings(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, com.the_coffe_coders.fastestlap.domain.constructor.StandingsAPIResponse.class);
    }

    public RaceWeekAPIresponse parseRaceWeek(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, RaceWeekAPIresponse.class);
    }

    public ResultsAPIResponse parseRaceResults(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, ResultsAPIResponse.class);
    }
}
