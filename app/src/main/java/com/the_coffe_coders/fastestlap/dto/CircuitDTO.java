package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class CircuitDTO {
    private String circuitId;
    private String url;
    private String circuitName;
    @SerializedName("Location")
    private LocationDTO location;
}