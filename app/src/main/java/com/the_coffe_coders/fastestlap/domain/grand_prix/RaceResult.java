package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.PrimaryKey;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

public class RaceResult {
    @PrimaryKey(autoGenerate = true)
    private int uid;
    private String position;
    private String points;
    private Driver driver;
    private Constructor constructor;
    private String grid;
    private String laps;
    private String status;

    public RaceResult(String position, String points, Driver driver, Constructor constructor, String grid, String laps, String status) {
        this.position = position;
        this.points = points;
        this.driver = driver;
        this.constructor = constructor;
        this.grid = grid;
        this.laps = laps;
        this.status = status;
    }

    public RaceResult() {

    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Constructor getConstructor() {
        return constructor;
    }

    public void setConstructor(Constructor constructor) {
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
}