package com.the_coffe_coders.fastestlap.dto;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RaceTableDTO {
    private String season;
    @SerializedName("Circuit")
    private CircuitDTO circuit;
    @SerializedName("Races")
    private List<RaceDTO> races;

    public RaceTableDTO(String season, String circuitId, List<RaceDTO> races) {
        this.season = season;
        this.circuit = circuit;
        races = races;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public CircuitDTO getCircuit() {
        return circuit;
    }

    public void setCircuitId(CircuitDTO circuitId) {
        this.circuit = circuit;
    }

    public List<RaceDTO> getRaces() {
        return races;
    }

    public void setRaces(List<RaceDTO> races) {
        races = races;
    }
}
