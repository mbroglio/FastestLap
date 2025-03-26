package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class ResultDTO {
    private String number;
    private String position;
    private String positionText;
    private String points;
    @SerializedName("Driver")
    private DriverDTO driver;
    @SerializedName("Constructor")
    private ConstructorDTO constructor;
    private String grid;
    private String laps;
    private String status;
}
