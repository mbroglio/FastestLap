package com.the_coffe_coders.fastestlap.domain.grand_prix;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity(tableName = "DriverStandings")
public class DriverStandings implements Parcelable {

    public static final Creator<DriverStandings> CREATOR = new Creator<DriverStandings>() {
        @Override
        public DriverStandings createFromParcel(Parcel in) {
            return new DriverStandings(in);
        }

        @Override
        public DriverStandings[] newArray(int size) {
            return new DriverStandings[size];
        }
    };
    @PrimaryKey(autoGenerate = true)
    private long uid;
    private String season;
    private String round;
    private List<DriverStandingsElement> driverStandingsElements;

    public DriverStandings(String season, String round, List<DriverStandingsElement> DriverStandings) {
        this.season = season;
        this.round = round;
        this.driverStandingsElements = new ArrayList<>();
        this.driverStandingsElements.addAll(DriverStandings);
    }

    public DriverStandings() {

    }

    protected DriverStandings(Parcel in) {
        uid = in.readLong();
        season = in.readString();
        round = in.readString();
        driverStandingsElements = in.createTypedArrayList(DriverStandingsElement.CREATOR);
    }

    //@Override
    public int describeContents() {
        return 0;
    }

    //@Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(uid);
        dest.writeString(season);
        dest.writeString(round);
        dest.writeTypedList(driverStandingsElements);
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

    public List<DriverStandingsElement> getDriverStandingsElements() {
        return driverStandingsElements;
    }

    public void setDriverStandingsElements(List<DriverStandingsElement> driverStandingsElements) {
        this.driverStandingsElements = driverStandingsElements;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DriverStandings that = (DriverStandings) o;
        return uid == that.uid && Objects.equals(season, that.season) && Objects.equals(round, that.round) && Objects.equals(driverStandingsElements, that.driverStandingsElements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, season, round, driverStandingsElements);
    }

    @Override
    public String toString() {
        return "DriverStandings{" +
                "season='" + season + '\'' +
                ", round='" + round + '\'' +
                ", DriverStandings=" + driverStandingsElements +
                '}';
    }
}
