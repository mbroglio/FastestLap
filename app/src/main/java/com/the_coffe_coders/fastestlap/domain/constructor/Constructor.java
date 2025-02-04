package com.the_coffe_coders.fastestlap.domain.constructor;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;

public class Constructor implements Parcelable {
    public static final Creator<Constructor> CREATOR = new Creator<Constructor>() {
        @Override
        public Constructor createFromParcel(Parcel in) {
            return new Constructor(in);
        }

        @Override
        public Constructor[] newArray(int size) {
            return new Constructor[size];
        }
    };
    private String constructorId;
    private String url;
    private String name;
    private String nationality;

    // Bio Data
    private String car_pic_url;
    private String chassis;
    private List<String> drivers;
    private String first_entry;
    private String full_name;
    private String hq;
    private String podiums;
    private String power_unit;
    private List<ConstructorHistory> team_history;
    private String team_logo_url;
    private String team_principal;
    private String wins;
    private String world_championships;

    public Constructor(String constructorId, String url, String name, String nationality) {
        this.constructorId = constructorId;
        this.url = url;
        this.name = name;
        this.nationality = nationality;
    }

    public Constructor(String name, String constructorId, String car_pic_url, String chassis, List<String> drivers, String first_entry, String full_name, String hq, String nationality, String podiums, String power_unit, List<ConstructorHistory> team_history, String team_logo_url, String team_principal, String wins, String world_championships) {
        this.name = name;
        this.constructorId = constructorId;
        this.car_pic_url = car_pic_url;
        this.chassis = chassis;
        this.drivers = drivers;
        this.first_entry = first_entry;
        this.full_name = full_name;
        this.hq = hq;
        this.nationality = nationality;
        this.podiums = podiums;
        this.power_unit = power_unit;
        this.team_history = team_history;
        this.team_logo_url = team_logo_url;
        this.team_principal = team_principal;
        this.wins = wins;
        this.world_championships = world_championships;
    }

    public Constructor() {

    }

    protected Constructor(Parcel in) {
        constructorId = in.readString();
        url = in.readString();
        name = in.readString();
        nationality = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(constructorId);
        dest.writeString(url);
        dest.writeString(name);
        dest.writeString(nationality);
    }

    public String getConstructorId() {
        return constructorId;
    }

    public void setConstructorId(String constructorId) {
        this.constructorId = constructorId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    @Override
    public String toString() {
        return "Constructor{" +
                "constructorId='" + constructorId + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", nationality='" + nationality + '\'' +
                '}';
    }

    public String getCar_pic_url() {
        return car_pic_url;
    }

    public void setCar_pic_url(String car_pic_url) {
        this.car_pic_url = car_pic_url;
    }

    public String getChassis() {
        return chassis;
    }

    public void setChassis(String chassis) {
        this.chassis = chassis;
    }

    public List<String> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<String> drivers) {
        this.drivers = drivers;
    }

    public String getFirst_entry() {
        return first_entry;
    }

    public void setFirst_entry(String first_entry) {
        this.first_entry = first_entry;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getHq() {
        return hq;
    }

    public void setHq(String hq) {
        this.hq = hq;
    }

    public String getPodiums() {
        return podiums;
    }

    public void setPodiums(String podiums) {
        this.podiums = podiums;
    }

    public String getPower_unit() {
        return power_unit;
    }

    public void setPower_unit(String power_unit) {
        this.power_unit = power_unit;
    }

    public List<ConstructorHistory> getTeam_history() {
        return team_history;
    }

    public void setTeam_history(List<ConstructorHistory> team_history) {
        this.team_history = team_history;
    }

    public String getTeam_logo_url() {
        return team_logo_url;
    }

    public void setTeam_logo_url(String team_logo_url) {
        this.team_logo_url = team_logo_url;
    }

    public String getTeam_principal() {
        return team_principal;
    }

    public void setTeam_principal(String team_principal) {
        this.team_principal = team_principal;
    }

    public String getWins() {
        return wins;
    }

    public void setWins(String wins) {
        this.wins = wins;
    }

    public String getWorld_championships() {
        return world_championships;
    }

    public void setWorld_championships(String world_championships) {
        this.world_championships = world_championships;
    }

    public String toStringDB() {
        return "Constructor{" +
                "constructorId='" + constructorId + '\'' +
                ", car_pic_url='" + car_pic_url + '\'' +
                ", chassis='" + chassis + '\'' +
                //", drivers=" + drivers.toString() +
                ", first_entry='" + first_entry + '\'' +
                ", full_name='" + full_name + '\'' +
                ", hq='" + hq + '\'' +
                ", nationality_db='" + nationality + '\'' +
                ", podiums='" + podiums + '\'' +
                ", power_unit='" + power_unit + '\'' +
                // ", team_history=" + team_history.toString() +
                ", team_logo_url='" + team_logo_url + '\'' +
                ", team_principal='" + team_principal + '\'' +
                ", wins='" + wins + '\'' +
                ", world_championships='" + world_championships + '\'' +
                '}';
    }

    public String getDriverOneId() {
        return drivers.get(0);
    }

    public String getDriverTwoId() {
        return drivers.get(1);
    }
}
