package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DriverStandingsListDTO {

    private String season;
    private String round;

    private List<DriverStandingsDTO> DriverStandings;

    public DriverStandingsListDTO(String season, String round, List<DriverStandingsDTO> driverStandings) {
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

    public List<DriverStandingsDTO> getDriverStandings() {
        return DriverStandings;
    }

    public void setDriverStandings(List<DriverStandingsDTO> driverStandings) {
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
