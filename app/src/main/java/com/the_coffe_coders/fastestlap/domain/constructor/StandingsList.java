package com.the_coffe_coders.fastestlap.domain.constructor;

import java.util.List;

public class StandingsList {
    private String season;
    private String round;
    private List<ConstructorStanding> ConstructorStandings;

    public StandingsList(String season, String round, List<ConstructorStanding> ConstructorStandings) {
        this.season = season;
        this.round = round;
        this.ConstructorStandings = ConstructorStandings;
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

    public List<ConstructorStanding> getConstructorStandings() {
        return ConstructorStandings;
    }

    public void setConstructorStandings(List<ConstructorStanding> standingLists) {
        this.ConstructorStandings = standingLists;
    }
}