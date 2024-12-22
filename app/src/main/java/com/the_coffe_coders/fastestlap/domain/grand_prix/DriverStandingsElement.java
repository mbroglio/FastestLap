package com.the_coffe_coders.fastestlap.domain.grand_prix;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.List;

public class DriverStandingsElement {
    private String position;
    private String positionText;
    private String points;
    private Driver Driver;
    private String wins;

    private List<Constructor> constructors;

    public DriverStandingsElement() {

    }

    public DriverStandingsElement(String position, String positionText, String points, com.the_coffe_coders.fastestlap.domain.driver.Driver driver, String wins) {
        this.position = position;
        this.positionText = positionText;
        this.points = points;
        this.Driver = driver;
        this.wins = wins;
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

    public com.the_coffe_coders.fastestlap.domain.driver.Driver getDriver() {
        return Driver;
    }

    public void setDriver(com.the_coffe_coders.fastestlap.domain.driver.Driver driver) {
        Driver = driver;
    }

    public String getWins() {
        return wins;
    }

    public void setWins(String wins) {
        this.wins = wins;
    }

    public List<Constructor> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<Constructor> constructors) {
        this.constructors = constructors;
    }

    @Override
    public String toString() {
        return "DriverStandingsElement{" +
                "position='" + position + '\'' +
                ", positionText='" + positionText + '\'' +
                ", points='" + points + '\'' +
                ", Driver=" + Driver +
                ", wins='" + wins + '\'' +
                ", constructors=" + constructors +
                '}';
    }
}
