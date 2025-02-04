package com.the_coffe_coders.fastestlap.domain.constructor;

public class ConstructorHistory {
    private String podiums;
    private String points;
    private String position;
    private String wins;
    private String year;

    public ConstructorHistory(String podiums, String points, String position, String wins, String year) {
        this.podiums = podiums;
        this.points = points;
        this.position = position;
        this.wins = wins;
        this.year = year;
    }

    public ConstructorHistory() {
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
        return "ConstructorHistory{" +
                "podiums='" + podiums + '\'' +
                ", points='" + points + '\'' +
                ", position='" + position + '\'' +
                ", wins='" + wins + '\'' +
                ", year='" + year + '\'' +
                '}';
    }
}
