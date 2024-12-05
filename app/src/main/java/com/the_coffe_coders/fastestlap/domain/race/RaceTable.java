package com.the_coffe_coders.fastestlap.domain.race;

import java.util.List;

public class RaceTable {
    private String season;
    private String circuitId;
    private List<Race> Races;

    public RaceTable(String season, String circuitId, List<Race> races) {
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

    public List<Race> getRaces() {
        return Races;
    }

    public void setRaces(List<Race> races) {
        Races = races;
    }
}
