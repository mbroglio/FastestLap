package com.the_coffe_coders.fastestlap.data.dto;

public class LocationDTO {

    private String lat;
    private String _long;
    private String locality;
    private String country;

    public LocationDTO(String lat, String _long, String locality, String country) {
        this.lat = lat;
        this._long = _long;
        this.locality = locality;
        this.country = country;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLong() {
        return _long;
    }

    public void setLong(String _long) {
        this._long = _long;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return "LocationDTO{" +
                "lat='" + lat + '\'' +
                ", _long='" + _long + '\'' +
                ", locality='" + locality + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
