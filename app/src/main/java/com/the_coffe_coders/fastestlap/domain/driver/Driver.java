package com.the_coffe_coders.fastestlap.domain.driver;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "Driver")
public class Driver implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private long uid;
    private String driverId;
    private String permanentNumber;
    private String code;
    private String url;
    private String givenName;
    private String familyName;
    private String dateOfBirth;
    private String nationality;

    /*public Driver(String driverId, String permanentNumber, String code, String url, String givenName, String familyName, String dateOfBirth, String nationality) {
        this.driverId = driverId;
        this.permanentNumber = permanentNumber;
        this.code = code;
        this.url = url;
        this.givenName = givenName;
        this.familyName = familyName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
    }*/

    public Driver() {

    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getPermanentNumber() {
        return permanentNumber;
    }

    public void setPermanentNumber(String permanentNumber) {
        this.permanentNumber = permanentNumber;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Driver)) return false;
        Driver driver = (Driver) o;
        return Objects.equals(driverId, driver.driverId) && Objects.equals(permanentNumber, driver.permanentNumber) && Objects.equals(code, driver.code) && Objects.equals(url, driver.url) && Objects.equals(givenName, driver.givenName) && Objects.equals(familyName, driver.familyName) && Objects.equals(dateOfBirth, driver.dateOfBirth) && Objects.equals(nationality, driver.nationality);
    }

    @Override
    public int hashCode() {
        return Objects.hash(driverId, permanentNumber, code, url, givenName, familyName, dateOfBirth, nationality);
    }

    @Override
    public String toString() {
        return "Driver{" +
                "driverId='" + driverId + '\'' +
                ", permanentNumber='" + permanentNumber + '\'' +
                ", code='" + code + '\'' +
                ", url='" + url + '\'' +
                ", givenName='" + givenName + '\'' +
                ", familyName='" + familyName + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", nationality='" + nationality + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeLong(uid);
        parcel.writeString(driverId);
        parcel.writeString(permanentNumber);
        parcel.writeString(code);
        parcel.writeString(url);
        parcel.writeString(givenName);
        parcel.writeString(familyName);
        parcel.writeString(dateOfBirth);
        parcel.writeString(nationality);
    }

    public void readFromParcel(Parcel parcel) {
        this.uid = parcel.readLong();
        this.driverId = parcel.readString();
        this.permanentNumber = parcel.readString();
        this.code = parcel.readString();
        this.url = parcel.readString();
        this.givenName = parcel.readString();
        this.familyName = parcel.readString();
        this.dateOfBirth = parcel.readString();
        this.nationality = parcel.readString();
    }

    protected Driver(Parcel in) {
        this.uid = in.readLong();
        this.driverId = in.readString();
        this.permanentNumber = in.readString();
        this.code = in.readString();
        this.url = in.readString();
        this.givenName = in.readString();
        this.familyName = in.readString();
        this.dateOfBirth = in.readString();
        this.nationality = in.readString();
    }

    public static final Parcelable.Creator<Driver> CREATOR = new Parcelable.Creator<Driver>() {
        @Override
        public Driver createFromParcel(Parcel source) {
            return new Driver(source);
        }

        @Override
        public Driver[] newArray(int size) {
            return new Driver[size];
        }
    };
}
