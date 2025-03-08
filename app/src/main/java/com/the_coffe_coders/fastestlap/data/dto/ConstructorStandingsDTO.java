package com.the_coffe_coders.fastestlap.data.dto;

import java.util.List;

public class ConstructorStandingsDTO {
    private String season;
    private String round;
    private List<ConstructorStandingsElementDTO> ConstructorStandings;

    public ConstructorStandingsDTO(String season, String round, List<ConstructorStandingsElementDTO> ConstructorStandings) {
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

    public List<ConstructorStandingsElementDTO> getConstructorStandings() {
        return ConstructorStandings;
    }

    public void setConstructorStandings(List<ConstructorStandingsElementDTO> standingLists) {
        this.ConstructorStandings = standingLists;
    }

    @Override
    public String toString() {
        return "ConstructorStandingsDTO{" +
                "season='" + season + '\'' +
                ", round='" + round + '\'' +
                ", ConstructorStandings=" + ConstructorStandings +
                '}';
    }
}