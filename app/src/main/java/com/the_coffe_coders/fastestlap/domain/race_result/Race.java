package com.the_coffe_coders.fastestlap.domain.race_result;

import java.util.List;

import com.the_coffe_coders.fastestlap.domain.race_week.Circuit;

public class Race {
    private String season;
    private String round;
    private String url;
    private String raceName;
    private Circuit Circuit;
    private String date;
    private String time;
    public List<Result> Results;

    public Race(String season, String round, String url, String raceName, Circuit circuit, String date, String time, List<Result> results) {
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        Circuit = circuit;
        this.date = date;
        this.time = time;
        Results = results;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public Circuit getCircuit() {
        return Circuit;
    }

    public void setCircuit(Circuit circuit) {
        Circuit = circuit;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<Result> getResults() {
        return Results;
    }

    public void setResults(List<Result> results) {
        Results = results;
    }
}
