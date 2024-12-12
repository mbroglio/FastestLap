package com.the_coffe_coders.fastestlap.dto;

public class ResultsDTO {

    private String number;
    private String position;
    private String positionText;
    private String points;
    private DriverDTO Driver;
    private ConstructorDTO Constructor;
    private String grid;
    private String laps;
    private String status;

    public ResultsDTO(String number, String position, String positionText, String points, DriverDTO driver, ConstructorDTO constructor, String grid, String laps, String status){
        this.number = number;
        this.position = position;
        this.positionText = positionText;
        this.points = points;
        this.Driver = driver;
        this.Constructor = constructor;
        this.grid = grid;
        this.laps = laps;
        this.status = status;
    }

    public String getNumber() {
        return number;
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

    public DriverDTO getDriver() {
        return Driver;
    }

    public ConstructorDTO getConstructor() {
        return Constructor;
    }

    public String getGrid() {
        return grid;
    }

    public String getLaps() {
        return laps;
    }

    public String getStatus() {
        return status;
    }

}
