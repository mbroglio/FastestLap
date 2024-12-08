package com.the_coffe_coders.fastestlap.domain.race_result;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;

import java.util.List;

public class RaceTable {
    private String season;
    private String round;
    private List<Race> Races;

    public RaceTable(String season, String round, List<Race> races) {
        this.season = season;
        this.round = round;
        Races = races;
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

    public List<Race> getRaces() {
        return Races;
    }

    public void setRaces(List<Race> races) {
        Races = races;
    }
}
