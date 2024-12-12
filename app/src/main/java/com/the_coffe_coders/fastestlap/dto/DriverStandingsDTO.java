package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DriverStandingsDTO {

    private String position;
    private String positionText;
    private String points;
    private String wins;
    @SerializedName("Driver")
    private DriverDTO driver;
    @SerializedName("Constructors")
    private List<ConstructorDTO> constructors;

    public DriverStandingsDTO(String position, String positionText, String points, String wins, DriverDTO driver, List<ConstructorDTO> constructors){
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

    public String getPositionText() {
        return positionText;
    }

    public String getPoints() {
        return points;
    }

    public String getWins() {
        return wins;
    }

    public DriverDTO getDriver() {
        return driver;
    }

    public List<ConstructorDTO> getConstructors() {
        return constructors;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setPositionText(String positionText) {
        this.positionText = positionText;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public void setWins(String wins) {
        this.wins = wins;
    }

    public void setDriver(DriverDTO driver) {
        this.driver = driver;
    }

    public void setConstructors(List<ConstructorDTO> constructors) {
        this.constructors = constructors;
    }
}
