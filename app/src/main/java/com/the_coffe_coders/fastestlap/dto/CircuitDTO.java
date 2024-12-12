package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

public class CircuitDTO {
    private String circuitId;
    private String url;
    private String circuitName;
    @SerializedName("Location")
    private LocationDTO location;

    public CircuitDTO(String circuitId, String url, String circuitName, LocationDTO location) {
        this.circuitId = circuitId;
        this.url = url;
        this.circuitName = circuitName;
        this.location = location;
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

    public LocationDTO getLocation() {
        return location;
    }

    public void setLocationDTO (LocationDTO location) {
        this.location = location;
    }
}
