package com.the_coffe_coders.fastestlap.domain.driver;

import java.util.List;

public class StandingsList {
    private String season;
    private String round;
    private List<DriverStanding> DriverStandings;

    public StandingsList(String season, String round, List<DriverStanding> driverStandings) {
        this.season = season;
        this.round = round;
        this.DriverStandings = driverStandings;
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

    public List<DriverStanding> getDriverStandings() {
        return DriverStandings;
    }

    public void setDriverStandings(List<DriverStanding> driverStandings) {
        this.DriverStandings = driverStandings;
    }

    @Override
    public String toString() {
        return "StandingsList{" +
                "season='" + season + '\'' +
                ", round='" + round + '\'' +
                ", DriverStandings=" + DriverStandings +
                '}';
    }
}
