package com.the_coffe_coders.fastestlap.domain.grand_prix;


import java.util.List;

public class DriverStandings {
    private String season;
    private String round;
    private List<DriverStandingsElement> DriverStandings;

    public DriverStandings(String season, String round, List<DriverStandingsElement> DriverStandings) {
        this.season = season;
        this.round = round;
        this.DriverStandings = DriverStandings;
    }

    public DriverStandings() {

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

    public List<DriverStandingsElement> getDriverStandings() {
        return DriverStandings;
    }

    public void setDriverStandings(List<DriverStandingsElement> driverStandings) {
        DriverStandings = driverStandings;
    }

    @Override
    public String toString() {
        return "DriverStandings{" +
                "season='" + season + '\'' +
                ", round='" + round + '\'' +
                ", DriverStandings=" + DriverStandings +
                '}';
    }
}
