package com.the_coffe_coders.fastestlap.data.dto;

import com.google.gson.annotations.SerializedName;

public class ResultDTO {

    private String number;
    private String position;
    private String positionText;
    private String points;
    @SerializedName("Driver")
    private DriverDTO driver;
    @SerializedName("Constructor")
    private ConstructorDTO constructor;
    private String grid;
    private String laps;
    private String status;

    public ResultDTO(String number, String position, String positionText, String points, DriverDTO driver, ConstructorDTO constructor, String grid, String laps, String status) {
        this.number = number;
        this.position = position;
        this.positionText = positionText;
        this.points = points;
        this.driver = driver;
        this.constructor = constructor;
        this.grid = grid;
        this.laps = laps;
        this.status = status;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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

    public DriverDTO getDriver() {
        return driver;
    }

    public void setDriver(DriverDTO driver) {
        this.driver = driver;
    }

    public ConstructorDTO getConstructor() {
        return constructor;
    }

    public void setConstructor(ConstructorDTO constructor) {
        this.constructor = constructor;
    }

    public String getGrid() {
        return grid;
    }

    public void setGrid(String grid) {
        this.grid = grid;
    }

    public String getLaps() {
        return laps;
    }

    public void setLaps(String laps) {
        this.laps = laps;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "\nResultDTO{" +
                "\nnumber='" + number + '\'' +
                ", \nposition='" + position + '\'' +
                ", \npositionText='" + positionText + '\'' +
                ", \npoints='" + points + '\'' +
                ", \ndriver=" + driver +
                ", \nconstructor=" + constructor +
                ", \ngrid='" + grid + '\'' +
                ", \nlaps='" + laps + '\'' +
                ", \nstatus='" + status + '\'' +
                '}';
    }
}
