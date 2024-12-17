package com.the_coffe_coders.fastestlap.dto;

import java.util.List;

public class ConstructorStandingsListDTO {
    private String season;
    private String round;
    private List<ConstructorStandingsDTO> ConstructorStandings;

    public ConstructorStandingsListDTO(String season, String round, List<ConstructorStandingsDTO> ConstructorStandings) {
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

    public List<ConstructorStandingsDTO> getConstructorStandings() {
        return ConstructorStandings;
    }

    public void setConstructorStandings(List<ConstructorStandingsDTO> standingLists) {
        this.ConstructorStandings = standingLists;
    }
}