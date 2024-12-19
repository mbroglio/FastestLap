package com.the_coffe_coders.fastestlap.dto;

import java.util.List;

public class DriverStandingsDTO {

    private String season;
    private String round;

    private List<DriverStandingsElementDTO> DriverStandings;

    public DriverStandingsDTO(String season, String round, List<DriverStandingsElementDTO> driverStandings) {
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

    public List<DriverStandingsElementDTO> getDriverStandings() {
        return DriverStandings;
    }

    public void setDriverStandings(List<DriverStandingsElementDTO> driverStandings) {
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
