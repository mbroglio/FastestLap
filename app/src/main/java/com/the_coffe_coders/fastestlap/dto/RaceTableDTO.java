package com.the_coffe_coders.fastestlap.dto;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RaceTableDTO {
    private String season;
    private String round;
    @SerializedName("Circuit")
    private CircuitDTO circuit;
    @SerializedName("Races")
    private List<RaceDTO> races;

    public RaceTableDTO(String season, String round, CircuitDTO circuit, List<RaceDTO> races) {
        this.season = season;
        //this.circuit = circuit;
        this.races = races;
        this.round = round;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
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

    public void setCircuitId(CircuitDTO circuit) {
        this.circuit = circuit;
    }

    public List<RaceDTO> getRaces() {
        return races;
    }

    public void setRaces(List<RaceDTO> races) {
        this.races = races;
    }

    public RaceDTO getRace() {
        return races.get(0);
    }

    @Override
    public String toString() {
        return "\nRaceTableDTO{" +
                "\nseason='" + season + '\'' +
                ", \nround='" + round + '\'' +
                ", \ncircuit=" + circuit +
                ", \nraces=" + races +
                '}';
    }
}
