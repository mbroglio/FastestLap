package com.the_coffe_coders.fastestlap.domain;

import android.widget.ImageView;

import org.json.JSONObject;

public class Driver {
    private String wholeName;
    private String driverId;
    private String nationality;
    private int height;

    private int points;
    private int standingPosition;
    private int driverNumber;
    private String teamName;
    private ImageView driverPic;

    public Driver(String wholeName, String driverId, String nationality, int height, int points, int standingPosition, int driverNumber, String teamName, ImageView driverPic) {
        this.wholeName = wholeName;
        this.driverId = driverId;
        this.nationality = nationality;
        this.height = height;
        this.points = points;
        this.standingPosition = standingPosition;
        this.driverNumber = driverNumber;
        this.teamName = teamName;
        this.driverPic = driverPic;
    }

    public Driver(JSONObject driver){
        try {
            JSONObject driverDetails = driver.getJSONObject("Driver");
            this.driverId = driverDetails.getString("driverId");

            this.points = driver.getInt("points");
            this.standingPosition = driver.getInt("position");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getWholeName() {
        return wholeName;
    }

    public void setWholeName(String wholeName) {
        this.wholeName = wholeName;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return "Driver{" +
                "name='" + wholeName + '\'' +
                ", nationality='" + nationality + '\'' +
                ", height=" + height +
                ", points=" + points +
                ", standingPosition=" + standingPosition +
                ", driverNumber=" + driverNumber +
                ", teamName='" + teamName + '\'' +
                ", driverPic=" + driverPic +
                '}';
    }
}
