package com.the_coffe_coders.fastestlap.domain.grand_prix;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeeklyRaceTable {
    private String season;
    private String round;

    private String url;
    private String raceName;
    @SerializedName("Circuit")
    private Circuit circuit;
    private String date;
    private String time;
    @SerializedName("Results")
    List<Result> results;
    @SerializedName("Races")
    private List<WeeklyRace> weeklyRaces;

    public WeeklyRaceTable(String season, String round, String url, String raceName, Circuit circuit, String date, String time, List<Result> results, List<WeeklyRace> weeklyRaces) {
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        this.circuit = circuit;
        this.date = date;
        this.time = time;
        this.results = results;
        this.weeklyRaces = weeklyRaces;
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
        return circuit;
    }

    public void setCircuit(Circuit circuit) {
        this.circuit = circuit;
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
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public List<WeeklyRace> getWeeklyRaces() {
        return weeklyRaces;
    }

    public void setWeeklyRaces(List<WeeklyRace> weeklyRaces) {
        this.weeklyRaces = weeklyRaces;
    }
}
