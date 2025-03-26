package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DriverStandingsElementDTO {

    private String position;
    private String positionText;
    private String points;
    private String wins;
    @SerializedName("Driver")
    private DriverDTO driver;
    @SerializedName("Constructors")
    private List<ConstructorDTO> constructors;

    public DriverStandingsElementDTO(String position, String positionText, String points, String wins, DriverDTO driver, List<ConstructorDTO> constructors) {
        this.position = position;
        this.positionText = positionText;
        this.points = points;
        this.wins = wins;
        this.driver = driver;
        this.constructors = constructors;
    }
}
