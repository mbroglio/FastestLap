package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
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

    public ResultDTO(String number, String position, String positionText, String points, DriverDTO driver, ConstructorDTO constructor, String grid, String laps, String status) {
        this.number = number;
        this.position = position;
        this.positionText = positionText;
        this.points = points;
        this.driver = driver;
        this.constructor = constructor;
        this.grid = grid;
        this.laps = laps;
        this.status = status;
    }

}
