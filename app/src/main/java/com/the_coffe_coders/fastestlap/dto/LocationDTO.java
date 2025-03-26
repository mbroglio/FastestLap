package com.the_coffe_coders.fastestlap.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
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


}
