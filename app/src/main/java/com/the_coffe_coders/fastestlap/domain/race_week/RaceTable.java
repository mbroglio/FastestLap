package com.the_coffe_coders.fastestlap.domain.race_week;

import java.util.List;

public class RaceTable {
    private String season;
    private String circuitId;
    private List<Races> Races;

    public RaceTable(String season, String circuitId, List<com.the_coffe_coders.fastestlap.domain.race_week.Races> races) {
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

    public List<com.the_coffe_coders.fastestlap.domain.race_week.Races> getRaces() {
        return Races;
    }

    public void setRaces(List<com.the_coffe_coders.fastestlap.domain.race_week.Races> races) {
        Races = races;
    }
}
