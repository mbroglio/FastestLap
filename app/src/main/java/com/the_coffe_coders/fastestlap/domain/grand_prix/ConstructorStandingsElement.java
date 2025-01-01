package com.the_coffe_coders.fastestlap.domain.grand_prix;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

public class ConstructorStandingsElement {
    private String position;
    private String positionText;
    private String points;
    private Constructor Constructor;
    private String wins;

    public ConstructorStandingsElement() {
    }

    public ConstructorStandingsElement(String position, String positionText, String points, com.the_coffe_coders.fastestlap.domain.constructor.Constructor constructor, String wins) {
        this.position = position;
        this.positionText = positionText;
        this.points = points;
        this.Constructor = constructor;
        this.wins = wins;
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

    public com.the_coffe_coders.fastestlap.domain.constructor.Constructor getConstructor() {
        return Constructor;
    }

    public void setConstructor(com.the_coffe_coders.fastestlap.domain.constructor.Constructor constructor) {
        this.Constructor = constructor;
    }

    public String getWins() {
        return wins;
    }

    public void setWins(String wins) {
        this.wins = wins;
    }

    @Override
    public String toString() {
        return "ConstructorStandingsElement{" +
                "position='" + position + '\'' +
                ", positionText='" + positionText + '\'' +
                ", points='" + points + '\'' +
                ", Constructor=" + Constructor +
                ", wins='" + wins + '\'' +
                '}';
    }
}
