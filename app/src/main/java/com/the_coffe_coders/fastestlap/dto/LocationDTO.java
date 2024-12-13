package com.the_coffe_coders.fastestlap.dto;

public class LocationDTO {

    private String lat;
    private String _long;
    private String locality;
    private String country;

    public LocationDTO(String lat, String _long, String locality, String country){
        this.lat = lat;
        this._long = _long;
        this.locality = locality;
        this.country = country;
    }

    public String getLat() {
        return lat;
    }

    public String getLong() {
        return _long;
    }

    public String getLocality() {
        return locality;
    }

    public String getCountry() {
        return country;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public void setLong(String _long) {
        this._long = _long;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
