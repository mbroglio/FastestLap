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
public class QualifyingResultDTO {
    private String number;
    private String position;
    @SerializedName("Driver")
    private DriverDTO driver;
    @SerializedName("Constructor")
    private ConstructorDTO constructor;
    @SerializedName("Q1")
    private String q1;
    @SerializedName("Q2")
    private String q2;
    @SerializedName("Q3")
    private String q3;
}
