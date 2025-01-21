package com.the_coffe_coders.fastestlap.domain.grand_prix;

public class CircuitHistory {
    private String[] podium_drivers;
    private String[] podium_teams;
    private String year;

    public CircuitHistory(String[] podium_drivers, String[] podium_teams, String year) {
        this.podium_drivers = podium_drivers;
        this.podium_teams = podium_teams;
        this.year = year;
    }

    public CircuitHistory() {
    }

    public String[] getPodium_drivers() {
        return podium_drivers;
    }

    public void setPodium_drivers(String[] podium_drivers) {
        this.podium_drivers = podium_drivers;
    }

    public String[] getPodium_teams() {
        return podium_teams;
    }

    public void setPodium_teams(String[] podium_teams) {
        this.podium_teams = podium_teams;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
