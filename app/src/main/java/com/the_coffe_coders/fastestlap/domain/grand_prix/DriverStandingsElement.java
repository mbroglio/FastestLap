package com.the_coffe_coders.fastestlap.domain.grand_prix;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.List;

//@Entity(tableName = "DriverStandingsElement")
public class DriverStandingsElement implements Parcelable {
    private String position;
    private String positionText;
    private String points;
    private Driver driver;
    private String wins;

    private List<Constructor> constructors;

    public DriverStandingsElement() {

    }

    public DriverStandingsElement(Driver driver, String position) {//TODO REMOVE CONSTRUCTOR
        this.driver = driver;
        this.position = position;
        this.points = "20";
    }

    public DriverStandingsElement(String position, String positionText, String points, com.the_coffe_coders.fastestlap.domain.driver.Driver driver, String wins) {
        this.position = position;
        this.positionText = positionText;
        this.points = points;
        this.driver = driver;
        this.wins = wins;
    }

    //PARCELABLE IMPLEMENTATION
    public DriverStandingsElement(Parcel in) {
        position = in.readString();
        positionText = in.readString();
        points = in.readString();
        driver = in.readParcelable(Driver.class.getClassLoader());
        wins = in.readString();
    }

    public static final Creator<DriverStandingsElement> CREATOR = new Creator<DriverStandingsElement>() {
        @Override
        public DriverStandingsElement createFromParcel(Parcel in) {
            return new DriverStandingsElement(in);
        }

        @Override
        public DriverStandingsElement[] newArray(int size) {
            return new DriverStandingsElement[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(this.position);
        dest.writeString(this.positionText);
        dest.writeString(this.points);
        dest.writeParcelable(this.driver, flags);
        dest.writeString(this.wins);
        dest.writeTypedList(this.constructors);
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

    public com.the_coffe_coders.fastestlap.domain.driver.Driver getDriver() {
        return driver;
    }

    public void setDriver(com.the_coffe_coders.fastestlap.domain.driver.Driver driver) {
        this.driver = driver;
    }

    public String getWins() {
        return wins;
    }

    public void setWins(String wins) {
        this.wins = wins;
    }

    public List<Constructor> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<Constructor> constructors) {
        this.constructors = constructors;
    }

    @Override
    public String toString() {
        return "DriverStandingsElement{" +
                "position='" + position + '\'' +
                ", positionText='" + positionText + '\'' +
                ", points='" + points + '\'' +
                ", Driver=" + driver +
                ", wins='" + wins + '\'' +
                ", constructors=" + constructors +
                '}';
    }

}
