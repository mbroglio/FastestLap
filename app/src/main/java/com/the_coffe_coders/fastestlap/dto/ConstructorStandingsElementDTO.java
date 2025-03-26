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
public class ConstructorStandingsElementDTO {
    private String position;
    private String positionText;
    private String points;
    private String wins;
    @SerializedName("Constructor")
    private ConstructorDTO constructor;
}
