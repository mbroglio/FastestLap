package com.the_coffe_coders.fastestlap.domain.constructor;

import java.util.List;

public class StandingsTable {
    private String season;
    private String round;
    private List<StandingsList> StandingsLists;

    public StandingsTable(String season, String round, List<StandingsList> StandingsLists) {
        this.season = season;
        this.round = round;
        this.StandingsLists = StandingsLists;
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

    public List<StandingsList> getStandingsLists() {
        return StandingsLists;
    }

    public void setStandingsLists(List<StandingsList> standingsLists) {
        StandingsLists = standingsLists;
    }
}
