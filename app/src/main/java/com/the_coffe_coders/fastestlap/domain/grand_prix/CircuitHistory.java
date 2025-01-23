package com.the_coffe_coders.fastestlap.domain.grand_prix;

import java.util.List;

public class CircuitHistory {
    private List<String> podium;
    private List<String> team;
    private String year;

    public CircuitHistory(List<String> podium, List<String> team, String year) {
        this.podium = podium;
        this.team = team;
        this.year = year;
    }

    public CircuitHistory() {
    }

    public List<String> getTeam() {
        return team;
    }

    public void setTeam(List<String> team) {
        this.team = team;
    }

    public List<String> getPodium() {
        return podium;
    }

    public void setPodium(List<String> podium) {
        this.podium = podium;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return "CircuitHistory{" +
                "podium=" + podium.toString() +
                ", team=" + team.toString() +
                ", year='" + year + '\'' +
                '}';
    }
}
