package com.the_coffe_coders.fastestlap.domain.driver;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

import java.util.List;

public class DriverStanding {
    private String position;
    private String positionText;
    private String points;
    private String wins;
    private Driver Driver;
    private List<Constructor> Constructors;

    public DriverStanding(String position, String positionText, String points, String wins, Driver Driver, List<Constructor> Constructors) {
        this.position = position;
        this.positionText = positionText;
        this.points = points;
        this.wins = wins;
        this.Driver = Driver;
        this.Constructors = Constructors;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPositionText() {
        return positionText;
    }

    public void setPositionText(String positionText) {
        this.positionText = positionText;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public String getWins() {
        return wins;
    }

    public void setWins(String wins) {
        this.wins = wins;
    }

    public Driver getDriver() {
        return Driver;
    }

    public void setDriver(Driver driver) {
        this.Driver = driver;
    }

    public List<Constructor> getConstructors() {
        return Constructors;
    }

    public void setConstructors(List<Constructor> constructors) {
        this.Constructors = constructors;
    }

    @Override
    public String toString() {
        return "DriverStanding{" +
                "position='" + position + '\'' +
                ", positionText='" + positionText + '\'' +
                ", points='" + points + '\'' +
                ", wins='" + wins + '\'' +
                ", driver=" + Driver +
                ", constructors=" + Constructors +
                '}';
    }
}
