package com.the_coffe_coders.fastestlap.dto;

import com.the_coffe_coders.fastestlap.domain.race.Location;

public class CircuitDTO {
    private String circuitId;
    private String url;
    private String circuitName;
    private Location Location;

    public CircuitDTO(String circuitId, String url, String circuitName, com.the_coffe_coders.fastestlap.domain.race.Location location) {
        this.circuitId = circuitId;
        this.url = url;
        this.circuitName = circuitName;
        Location = location;
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

    public com.the_coffe_coders.fastestlap.domain.race.Location getLocation() {
        return Location;
    }

    public void setLocation(com.the_coffe_coders.fastestlap.domain.race.Location location) {
        Location = location;
    }
}
