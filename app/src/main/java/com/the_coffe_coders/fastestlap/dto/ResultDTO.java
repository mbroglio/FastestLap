package com.the_coffe_coders.fastestlap.dto;

public class ResultDTO {

    private String number;
    private String position;
    private String positionText;
    private String points;
    private DriverDTO Driver;
    private ConstructorDTO Constructor;
    private String grid;
    private String laps;
    private String status;

    public ResultDTO(String number, String position, String positionText, String points, DriverDTO driver, ConstructorDTO constructor, String grid, String laps, String status){
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

    public void setNumber(String number) {
        this.number = number;
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

    public void setDriver(DriverDTO driver) {
        this.Driver = driver;
    }

    public void setConstructor(ConstructorDTO constructor) {
        this.Constructor = constructor;
    }

    public void setGrid(String grid) {
        this.grid = grid;
    }

    public void setLaps(String laps) {
        this.laps = laps;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
