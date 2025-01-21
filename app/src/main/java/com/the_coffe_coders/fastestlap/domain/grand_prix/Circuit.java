package com.the_coffe_coders.fastestlap.domain.grand_prix;

import java.util.Arrays;

public class Circuit {
    private String circuitId;
    private String url;
    private String circuitName;
    private Location location;

    // Bio Data
    private String circuit_full_layout_url;
    private CircuitHistory[] circuit_history;
    private String circuit_minimal_layout_url;
    private String circuit_pic_url;
    private String country;
    private String gp_long_name;
    private String gp_lap_record;
    private String laps;
    private String length;
    private String race_distance;

    public Circuit(String circuitId, String url, String circuitName, com.the_coffe_coders.fastestlap.domain.grand_prix.Location location) {
        this.circuitId = circuitId;
        this.url = url;
        this.circuitName = circuitName;
        this.location = location;
    }

    public Circuit(String circuitId, String circuit_full_layout_url, CircuitHistory[] circuit_history, String circuit_minimal_layout_url, String circuit_pic_url, String country, String gp_long_name, String gp_lap_record, String laps, String length, String race_distance) {
        this.circuitId = circuitId;
        this.circuit_full_layout_url = circuit_full_layout_url;
        this.circuit_history = circuit_history;
        this.circuit_minimal_layout_url = circuit_minimal_layout_url;
        this.circuit_pic_url = circuit_pic_url;
        this.country = country;
        this.gp_long_name = gp_long_name;
        this.gp_lap_record = gp_lap_record;
        this.laps = laps;
        this.length = length;
        this.race_distance = race_distance;
    }

    public Circuit() {

    }

    public String getCircuitId() {
        return circuitId;
    }

    public void setCircuitId(String circuitId) {
        this.circuitId = circuitId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public void setCircuitName(String circuitName) {
        this.circuitName = circuitName;
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
                "circuitId='" + circuitId + '\'' +
                ", url='" + url + '\'' +
                ", circuitName='" + circuitName + '\'' +
                ", location=" + location +
                '}';
    }

    public String getCircuit_full_layout_url() {
        return circuit_full_layout_url;
    }

    public void setCircuit_full_layout_url(String circuit_full_layout_url) {
        this.circuit_full_layout_url = circuit_full_layout_url;
    }

    public CircuitHistory[] getCircuit_history() {
        return circuit_history;
    }

    public void setCircuit_history(CircuitHistory[] circuit_history) {
        this.circuit_history = circuit_history;
    }

    public String getCircuit_minimal_layout_url() {
        return circuit_minimal_layout_url;
    }

    public void setCircuit_minimal_layout_url(String circuit_minimal_layout_url) {
        this.circuit_minimal_layout_url = circuit_minimal_layout_url;
    }

    public String getCircuit_pic_url() {
        return circuit_pic_url;
    }

    public void setCircuit_pic_url(String circuit_pic_url) {
        this.circuit_pic_url = circuit_pic_url;
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

    public String getGp_lap_record() {
        return gp_lap_record;
    }

    public void setGp_lap_record(String gp_lap_record) {
        this.gp_lap_record = gp_lap_record;
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
        return "Circuit{" +
                "circuitId='" + circuitId + '\'' +
                ", circuit_full_layout_url='" + circuit_full_layout_url + '\'' +
                ", circuit_history=" + Arrays.toString(circuit_history) +
                ", circuit_minimal_layout_url='" + circuit_minimal_layout_url + '\'' +
                ", circuit_pic_url='" + circuit_pic_url + '\'' +
                ", country='" + country + '\'' +
                ", gp_long_name='" + gp_long_name + '\'' +
                ", gp_lap_record='" + gp_lap_record + '\'' +
                ", laps='" + laps + '\'' +
                ", length='" + length + '\'' +
                ", race_distance='" + race_distance + '\'' +
                '}';
    }
}
