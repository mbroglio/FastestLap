package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RaceDTO {
    @SerializedName("Results")
    List<ResultDTO> results;
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

    public RaceDTO(String season, String round, String url, String raceName, CircuitDTO circuit, String date, String time, PracticeDTO firstPractice, PracticeDTO secondPractice, PracticeDTO thirdPractice, QualifyingDTO qualifying, SprintQualifyingDTO sprintQualifying, SprintDTO sprint, List<ResultDTO> resultDTO) {
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        this.circuit = circuit;
        this.date = date;
        this.time = time;
        this.firstPractice = firstPractice;
        this.secondPractice = secondPractice;
        this.thirdPractice = thirdPractice;
        this.qualifying = qualifying;
        this.sprintQualifying = sprintQualifying;
        this.sprint = sprint;
        this.results = resultDTO;
    }


}
