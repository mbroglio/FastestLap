package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

public class ConstructorStandingsElementDTO {

    private String position;
    private String positionText;
    private String points;
    private String wins;
    @SerializedName("Constructor")
    private ConstructorDTO constructor;

    public ConstructorStandingsElementDTO(String position, String positionText, String points, String wins, ConstructorDTO constructor) {
        this.position = position;
        this.positionText = positionText;
        this.wins = wins;
        this.constructor = constructor;
    }

    public String getPosition() {
        return this.position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPositionText() {
        return this.positionText;
    }

    public void setPositionText(String positionText) {
        this.positionText = positionText;
    }

    public String getPoints() {
        return this.points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public String getWins() {
        return this.wins;
    }

    public void setWins(String wins) {
        this.wins = wins;
    }

    public ConstructorDTO getConstructor() {
        return this.constructor;
    }

    public void setConstructor(ConstructorDTO constructor) {
        this.constructor = constructor;
    }

    @Override
    public String toString() {
        return "ConstructorStandingsElementDTO{" +
                "position='" + position + '\'' +
                ", positionText='" + positionText + '\'' +
                ", points='" + points + '\'' +
                ", wins='" + wins + '\'' +
                ", constructor=" + constructor +
                '}';
    }
}
