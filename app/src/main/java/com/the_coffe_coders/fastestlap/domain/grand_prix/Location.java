package com.the_coffe_coders.fastestlap.domain.grand_prix;

public class Location {
    private String lat;
    private String _long;
    private String locality;
    private String country;

    public Location(String lat, String _long, String locality, String country) {
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

    public String get_long() {
        return _long;
    }

    public void set_long(String _long) {
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
}
