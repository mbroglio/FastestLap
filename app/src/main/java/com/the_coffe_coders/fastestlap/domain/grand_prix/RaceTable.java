package com.the_coffe_coders.fastestlap.domain.grand_prix;

import java.util.List;

public class RaceTable {
    private String season;
    private String circuitId;
    private List<WeeklyRace> Races;

    public RaceTable(String season, String circuitId, List<WeeklyRace> races) {
        this.season = season;
        this.circuitId = circuitId;
        Races = races;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getCircuitId() {
        return circuitId;
    }

    public void setCircuitId(String circuitId) {
        this.circuitId = circuitId;
    }

    public List<WeeklyRace> getRaces() {
        return Races;
    }

    public void setRaces(List<WeeklyRace> races) {
        Races = races;
    }
}
