package com.the_coffe_coders.fastestlap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class LocationDTO {
    private String lat;
    private String _long;
    private String locality;
    private String country;
}
