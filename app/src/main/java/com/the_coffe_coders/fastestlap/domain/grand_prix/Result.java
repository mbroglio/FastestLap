package com.the_coffe_coders.fastestlap.domain.grand_prix;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

public class Result {
    private String number;
    private String position;
    private String positionText;
    private String points;
    private Driver Driver;
    private Constructor Constructor;
    private String grid;
    private String laps;
    private String status;

    public Result(String number, String position, String positionText, String points, Driver driver, Constructor constructor, String grid, String laps, String status) {
        this.number = number;
        this.position = position;
        this.positionText = positionText;
        this.points = points;
        Driver = driver;
        Constructor = constructor;
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

    public Driver getDriver() {
        return Driver;
    }

    public void setDriver(Driver driver) {
        Driver = driver;
    }

    public Constructor getConstructor() {
        return Constructor;
    }

    public void setConstructor(Constructor constructor) {
        Constructor = constructor;
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
}