package com.the_coffe_coders.fastestlap.domain.driver;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.List;
import java.util.Objects;

@Entity(tableName = "Driver")
public class Driver implements Parcelable {
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

    // Bio Data
    private String best_result;
    private String birth_place;
    private String championships;
    @Ignore // Check if it generates errors
    private List<DriverHistory> driver_history;
    private String driver_pic_url;
    private String height;
    private String podiums;
    private String racing_number_pic_url;
    private String team_id;
    private String weight;

    public Driver(String driverId, String givenName, String familyName, String dateOfBirth, String nationality, String best_result, String birth_place, String championships, List<DriverHistory> driver_history, String driver_pic_url, String height, String podiums, String racing_number_pic_url, String team_id, String weight) {
        this.driverId = driverId;
        this.givenName = givenName;
        this.familyName = familyName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.best_result = best_result;
        this.birth_place = birth_place;
        this.championships = championships;
        this.driver_history = driver_history;
        this.driver_pic_url = driver_pic_url;
        this.height = height;
        this.podiums = podiums;
        this.racing_number_pic_url = racing_number_pic_url;
        this.team_id = team_id;
        this.weight = weight;
    }

    public Driver() {

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

    public String getBest_result() {
        return best_result;
    }

    public void setBest_result(String best_result) {
        this.best_result = best_result;
    }

    public String getBirth_place() {
        return birth_place;
    }

    public void setBirth_place(String birth_place) {
        this.birth_place = birth_place;
    }

    public String getChampionships() {
        return championships;
    }

    public void setChampionships(String championships) {
        this.championships = championships;
    }

    public List<DriverHistory> getDriver_history() {
        return driver_history;
    }

    public void setDriver_history(List<DriverHistory> driver_history) {
        this.driver_history = driver_history;
    }

    public String getDriver_pic_url() {
        return driver_pic_url;
    }

    public void setDriver_pic_url(String driver_pic_url) {
        this.driver_pic_url = driver_pic_url;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getPodiums() {
        return podiums;
    }

    public void setPodiums(String podiums) {
        this.podiums = podiums;
    }

    public String getRacing_number_pic_url() {
        return racing_number_pic_url;
    }

    public void setRacing_number_pic_url(String racing_number_pic_url) {
        this.racing_number_pic_url = racing_number_pic_url;
    }

    public String getTeam_id() {
        return team_id;
    }

    public void setTeam_id(String team_id) {
        this.team_id = team_id;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String toStringDB() {
        return "Driver{" +
                "best_result='" + best_result + '\'' +
                ", birth_place='" + birth_place + '\'' +
                ", championships='" + championships + '\'' +
                ", driver_pic_url='" + driver_pic_url + '\'' +
                ", driver_history=" + driver_history.toString() +
                ", height='" + height + '\'' +
                ", nationality_db='" + nationality + '\'' +
                ", podiums='" + podiums + '\'' +
                ", racing_number_pic_url='" + racing_number_pic_url + '\'' +
                ", team_id='" + team_id + '\'' +
                ", weight='" + weight + '\'' +
                ", driverId='" + driverId + '\'' +
                '}';
    }
}
