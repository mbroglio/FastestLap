package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.AllArgsConstructor;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class DriverStandingsElementDTO {
    private String position;
    private String positionText;
    private String points;
    private String wins;
    @SerializedName("Driver")
    private DriverDTO driver;
    @SerializedName("Constructors")
    private List<ConstructorDTO> constructors;
}
