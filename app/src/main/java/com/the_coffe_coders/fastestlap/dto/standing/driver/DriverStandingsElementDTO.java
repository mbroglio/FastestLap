package com.the_coffe_coders.fastestlap.dto.standing.driver;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.dto.standing.constructor.ConstructorDTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
