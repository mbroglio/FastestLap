package com.the_coffe_coders.fastestlap.dto;

import java.util.List;

public class ConstructorStandingsTableDTO {
    private String season;
    private String round;
    private List<ConstructorStandingsListDTO> StandingsLists;

    public ConstructorStandingsTableDTO(String season, String round, List<ConstructorStandingsListDTO> StandingsLists) {
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

    public List<ConstructorStandingsListDTO> getStandingsLists() {
        return StandingsLists;
    }

    public void setStandingsLists(List<ConstructorStandingsListDTO> standingsLists) {
        StandingsLists = standingsLists;
    }

    @Override
    public String toString() {
        return "StandingsTable{" +
                "season='" + season +
        ", round='" + round +
        ", StandingsLists=" + StandingsLists +
                '}';
    }
}