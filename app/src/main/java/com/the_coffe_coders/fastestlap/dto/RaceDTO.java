package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class RaceDTO {
    @SerializedName("Results")
    List<ResultDTO> results;
    @SerializedName("QualifyingResults")
    List<QualifyingResultDTO> qualifyingResults;
    @SerializedName("SprintResults")
    List<ResultDTO> sprintResults;
    private String season;
    private String round;
    private String url;
    private String raceName;
    @SerializedName("Circuit")
    private CircuitDTO circuit;
    private String date;
    private String time;
    @SerializedName("FirstPractice")
    private PracticeDTO firstPractice;
    @SerializedName("SecondPractice")
    private PracticeDTO secondPractice;
    @SerializedName("ThirdPractice")
    private PracticeDTO thirdPractice;
    @SerializedName("Qualifying")
    private QualifyingDTO qualifying;
    @SerializedName("SprintQualifying")
    private SprintQualifyingDTO sprintQualifying;
    @SerializedName("Sprint")
    private SprintDTO sprint;
}
