package com.the_coffe_coders.fastestlap.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DriverStandingsTableDTO {
    private String season;
    private String round;
    @SerializedName("StandingsLists")
    private List<DriverStandingsDTO> driverStandingsDTOS;

    public DriverStandingsTableDTO(String season, String round, List<DriverStandingsDTO> driverStandingsDTOS) {
        this.season = season;
        this.round = round;
        this.driverStandingsDTOS = driverStandingsDTOS;
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

    public List<DriverStandingsDTO> getStandingsLists() {
        return driverStandingsDTOS;
    }

    public void setStandingsLists(List<DriverStandingsDTO> driverStandingsDTOS) {
        this.driverStandingsDTOS = driverStandingsDTOS;
    }

    @Override
    public String toString() {
        return "StandingsTable{" +
                "season='" + season + '\'' +
                ", round='" + round + '\'' +
                ", StandingsLists=" + driverStandingsDTOS +
                '}';
    }
}
