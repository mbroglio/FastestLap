package com.the_coffe_coders.fastestlap.domain.driver;

public class DriverHistory {
    private String podiums;
    private String points;
    private String position;
    private String team;
    private String wins;
    private String year;

    public DriverHistory(String podiums, String points, String position, String team, String wins, String year) {
        this.podiums = podiums;
        this.points = points;
        this.position = position;
        this.team = team;
        this.wins = wins;
        this.year = year;
    }

    public DriverHistory() {
    }

    public String getPodiums() {
        return podiums;
    }

    public void setPodiums(String podiums) {
        this.podiums = podiums;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getWins() {
        return wins;
    }

    public void setWins(String wins) {
        this.wins = wins;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return "DriverHistory{" +
                "podiums='" + podiums + '\'' +
                ", points='" + points + '\'' +
                ", position='" + position + '\'' +
                ", team='" + team + '\'' +
                ", wins='" + wins + '\'' +
                ", year='" + year + '\'' +
                '}';
    }
}
