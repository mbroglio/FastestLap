package com.the_coffe_coders.fastestlap.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DriverStandingsElementDTO {

    private String position;
    private String positionText;
    private String points;
    private String wins;
    @SerializedName("Driver")
    private DriverDTO driver;
    @SerializedName("Constructors")
    private List<ConstructorDTO> constructors;

    public DriverStandingsElementDTO(String position, String positionText, String points, String wins, DriverDTO driver, List<ConstructorDTO> constructors) {
        this.position = position;
        this.positionText = positionText;
        this.points = points;
        this.wins = wins;
        this.driver = driver;
        this.constructors = constructors;
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

    public DriverDTO getDriver() {
        return driver;
    }

    public void setDriver(DriverDTO driver) {
        this.driver = driver;
    }

    public List<ConstructorDTO> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<ConstructorDTO> constructors) {
        this.constructors = constructors;
    }

    @Override
    public String toString() {
        return "DriverStandingsElementDTO{" +
                "position='" + position + '\'' +
                ", positionText='" + positionText + '\'' +
                ", points='" + points + '\'' +
                ", wins='" + wins + '\'' +
                ", driver=" + driver +
                ", constructors=" + constructors +
                '}';
    }
}
