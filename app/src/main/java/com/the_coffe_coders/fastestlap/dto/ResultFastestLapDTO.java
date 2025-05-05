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
public class ResultFastestLapDTO {
    private String rank;
    private String lap;
    @SerializedName("Time")
    private ResultTimeDTO time;
}
