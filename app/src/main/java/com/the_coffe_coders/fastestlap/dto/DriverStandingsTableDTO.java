package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DriverStandingsTableDTO {
    private String season;
    private String round;
    @SerializedName("StandingsLists")
    private List<DriverStandingsDTO> driverStandingsDTOS;

    public DriverStandingsTableDTO(String season, String round, List<DriverStandingsDTO> driverStandingsDTOS) {
        this.season = season;
        this.round = round;
        this.driverStandingsDTOS = driverStandingsDTOS;
    }
}
