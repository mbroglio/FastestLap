package com.the_coffe_coders.fastestlap.dto;

import java.util.List;

public class DriverStandingsDTO {

    private String position;
    private String positionText;
    private String points;
    private String wins;
    private DriverDTO driver;
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
}
