package com.the_coffe_coders.fastestlap.domain.grand_prix;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RaceTable {
    private String season;
    private String round;
    @SerializedName("Races")
    private List<WeeklyRaceTable> weeklyRacesTable;

    public RaceTable(String season, String round, List<WeeklyRaceTable> weeklyRacesTable) {
        this.season = season;
        this.round = round;
        this.weeklyRacesTable = this.weeklyRacesTable;
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

    public List<WeeklyRaceTable> getRaces() {
        return weeklyRacesTable;
    }

    public void setRaces(List<WeeklyRaceTable> weeklyRaces) {
        this.weeklyRacesTable = weeklyRaces;
    }
}
