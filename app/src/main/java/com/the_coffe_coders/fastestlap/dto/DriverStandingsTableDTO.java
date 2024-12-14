package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DriverStandingsTableDTO {
    private String season;
    private String round;
    @SerializedName("StandingsLists")
    private List<DriverStandingsListDTO> driverStandingsListDTOS;

    public DriverStandingsTableDTO(String season, String round, List<DriverStandingsListDTO> driverStandingsListDTOS) {
        this.season = season;
        this.round = round;
        this.driverStandingsListDTOS = driverStandingsListDTOS;
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

    public List<DriverStandingsListDTO> getStandingsLists() {
        return driverStandingsListDTOS;
    }

    public void setStandingsLists(List<DriverStandingsListDTO> driverStandingsListDTOS) {
        this.driverStandingsListDTOS = driverStandingsListDTOS;
    }

    @Override
    public String toString() {
        return "StandingsTable{" +
                "season='" + season + '\'' +
                ", round='" + round + '\'' +
                ", StandingsLists=" + driverStandingsListDTOS +
                '}';
    }
}
