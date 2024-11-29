package com.the_coffe_coders.fastestlap.domain;

import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public class Constructor {
    private String teamId;
    private String teamName;
    private String nationality;
    private int standingPosition;
    private int points;

    private Driver driver1;
    private Driver driver2;

    private ImageView teamLogo;
    private ImageView carPic;

    public Constructor(String teamId, String teamName, String nationality, int standingPosition, int points, Driver driver1, Driver driver2, ImageView teamLogo, ImageView carPic) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.nationality = nationality;
        this.standingPosition = standingPosition;
        this.points = points;
        this.driver1 = driver1;
        this.driver2 = driver2;
        this.teamLogo = teamLogo;
        this.carPic = carPic;
    }

    public Constructor(JSONObject constructor) {
        try {
            this.teamId = constructor.getJSONObject("Constructor").getString("constructorId");
            this.points = constructor.getInt("points");
            this.standingPosition = Integer.parseInt(constructor.getString("position"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public int getStandingPosition() {
        return standingPosition;
    }

    public void setStandingPosition(int standingPosition) {
        this.standingPosition = standingPosition;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public Driver getDriver1() {
        return driver1;
    }

    public void setDriver1(Driver driver1) {
        this.driver1 = driver1;
    }

    public Driver getDriver2() {
        return driver2;
    }

    public void setDriver2(Driver driver2) {
        this.driver2 = driver2;
    }

    public ImageView getTeamLogo() {
        return teamLogo;
    }

    public void setTeamLogo(ImageView teamLogo) {
        this.teamLogo = teamLogo;
    }

    public ImageView getCarPic() {
        return carPic;
    }

    public void setCarPic(ImageView carPic) {
        this.carPic = carPic;
    }

    @NonNull
    @Override
    public String toString() {
        return "Constructor{" +
                "teamName='" + teamName + '\'' +
                ", nationality='" + nationality + '\'' +
                ", standingPosition=" + standingPosition +
                ", points=" + points +
                ", driver1=" + driver1 +
                ", driver2=" + driver2 +
                ", teamLogo=" + teamLogo +
                ", carPic=" + carPic +
                '}';
    }
}
