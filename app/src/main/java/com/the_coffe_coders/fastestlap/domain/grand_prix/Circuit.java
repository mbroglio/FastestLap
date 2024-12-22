package com.the_coffe_coders.fastestlap.domain.grand_prix;

import com.google.gson.annotations.SerializedName;

public class Circuit {
    private String circuitId;
    private String url;
    private String circuitName;
    @SerializedName("Location")
    private Location location;

    public Circuit(String circuitId, String url, String circuitName, com.the_coffe_coders.fastestlap.domain.grand_prix.Location location) {
        this.circuitId = circuitId;
        this.url = url;
        this.circuitName = circuitName;
        this.location = location;
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
}
