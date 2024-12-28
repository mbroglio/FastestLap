package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;
@Entity(tableName = "ConstructorStandings")
public class ConstructorStandings {
    @PrimaryKey(autoGenerate = true)
    private int uid;
    private String season;
    private String round;
    private List<ConstructorStandingsElement> ConstructorStandings;

    public ConstructorStandings(String season, String round, List<ConstructorStandingsElement> ConstructorStandings) {
        this.season = season;
        this.round = round;
        this.ConstructorStandings = ConstructorStandings;
    }

    public ConstructorStandings() {

    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public List<ConstructorStandingsElement> getConstructorStandings() {
        return ConstructorStandings;
    }

    public void setConstructorStandings(List<ConstructorStandingsElement> constructorStandings) {
        ConstructorStandings = constructorStandings;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "ConstructorStandings{" +
                "season='" + season + '\'' +
                ", round='" + round + '\'' +
                ", ConstructorStandings=" + ConstructorStandings +
                '}';
    }
}
