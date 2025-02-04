package com.the_coffe_coders.fastestlap.domain.grand_prix;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Track {
    private String trackId;
    private String url;
    private String trackName;
    private Location location;

    // Bio Data
    private String track_full_layout_url;
    @SerializedName("circuit_history")
    private List<TrackHistory> track_history;
    private String track_minimal_layout_url;
    private String track_pic_url;
    private String country;
    private String gp_long_name;
    private String lap_record;
    private String laps;
    private String length;
    private String race_distance;

    public Track(String trackId, String url, String trackName, com.the_coffe_coders.fastestlap.domain.grand_prix.Location location) {
        this.trackId = trackId;
        this.url = url;
        this.trackName = trackName;
        this.location = location;
    }

    public Track(String trackId, String track_full_layout_url, List<TrackHistory> track_history, String track_minimal_layout_url, String track_pic_url, String country, String gp_long_name, String lap_record, String laps, String length, String race_distance) {
        this.trackId = trackId;
        this.track_full_layout_url = track_full_layout_url;
        this.track_history = track_history;
        this.track_minimal_layout_url = track_minimal_layout_url;
        this.track_pic_url = track_pic_url;
        this.country = country;
        this.gp_long_name = gp_long_name;
        this.lap_record = lap_record;
        this.laps = laps;
        this.length = length;
        this.race_distance = race_distance;
    }

    public Track() {

    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public com.the_coffe_coders.fastestlap.domain.grand_prix.Location getLocation() {
        return location;
    }

    public void setLocation(com.the_coffe_coders.fastestlap.domain.grand_prix.Location location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "Circuit{" +
                "circuitId='" + trackId + '\'' +
                ", url='" + url + '\'' +
                ", circuitName='" + trackName + '\'' +
                ", location=" + location +
                '}';
    }

    public String getTrack_full_layout_url() {
        return track_full_layout_url;
    }

    public void setTrack_full_layout_url(String track_full_layout_url) {
        this.track_full_layout_url = track_full_layout_url;
    }

    public List<TrackHistory> getTrack_history() {
        return track_history;
    }

    public void setTrack_history(List<TrackHistory> track_history) {
        this.track_history = track_history;
    }

    public String getTrack_minimal_layout_url() {
        return track_minimal_layout_url;
    }

    public void setTracl_minimal_layout_url(String track_minimal_layout_url) {
        this.track_minimal_layout_url = track_minimal_layout_url;
    }

    public String getTrack_pic_url() {
        return track_pic_url;
    }

    public void setTrack_pic_url(String track_pic_url) {
        this.track_pic_url = track_pic_url;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getGp_long_name() {
        return gp_long_name;
    }

    public void setGp_long_name(String gp_long_name) {
        this.gp_long_name = gp_long_name;
    }

    public String getLap_record() {
        return lap_record;
    }

    public void setLap_record(String lap_record) {
        this.lap_record = lap_record;
    }

    public String getLaps() {
        return laps;
    }

    public void setLaps(String laps) {
        this.laps = laps;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getRace_distance() {
        return race_distance;
    }

    public void setRace_distance(String race_distance) {
        this.race_distance = race_distance;
    }

    public String toStringDB() {
        return "track{" +
                "trackId='" + trackId + '\'' +
                ", track_full_layout_url='" + track_full_layout_url + '\'' +
                ", track_history=" + track_history +
                ", track_minimal_layout_url='" + track_minimal_layout_url + '\'' +
                ", track_pic_url='" + track_pic_url + '\'' +
                ", country='" + country + '\'' +
                ", gp_long_name='" + gp_long_name + '\'' +
                ", lap_record='" + lap_record + '\'' +
                ", laps='" + laps + '\'' +
                ", length='" + length + '\'' +
                ", race_distance='" + race_distance + '\'' +
                '}';
    }
}
