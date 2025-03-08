package com.the_coffe_coders.fastestlap.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

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

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public CircuitDTO getCircuit() {
        return circuit;
    }

    public void setCircuit(CircuitDTO circuit) {
        this.circuit = circuit;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public PracticeDTO getFirstPractice() {
        return firstPractice;
    }

    public void setFirstPractice(PracticeDTO firstPractice) {
        this.firstPractice = firstPractice;
    }

    public PracticeDTO getSecondPractice() {
        return secondPractice;
    }

    public void setSecondPractice(PracticeDTO secondPractice) {
        this.secondPractice = secondPractice;
    }

    public PracticeDTO getThirdPractice() {
        return thirdPractice;
    }

    public void setThirdPractice(PracticeDTO thirdPractice) {
        this.thirdPractice = thirdPractice;
    }

    public QualifyingDTO getQualifying() {
        return qualifying;
    }

    public void setQualifying(QualifyingDTO qualifying) {
        this.qualifying = qualifying;
    }

    public SprintQualifyingDTO getSprintQualifying() {
        return sprintQualifying;
    }

    public void setSprintQualifying(SprintQualifyingDTO sprintQualifying) {
        this.sprintQualifying = sprintQualifying;
    }

    public SprintDTO getSprint() {
        return sprint;
    }

    public void setSprint(SprintDTO sprint) {
        this.sprint = sprint;
    }

    public List<ResultDTO> getResults() {
        return results;
    }

    public void setResults(List<ResultDTO> results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "\nRaceDTO{" +
                "\nseason='" + season + '\'' +
                ", \nround='" + round + '\'' +
                ", \nurl='" + url + '\'' +
                ", \nraceName='" + raceName + '\'' +
                ", \ncircuit=" + circuit +
                ", \ndate='" + date + '\'' +
                ", \ntime='" + time + '\'' +
                ", \nfirstPractice=" + firstPractice +
                ", \nsecondPractice=" + secondPractice +
                ", \nthirdPractice=" + thirdPractice +
                ", \nqualifying=" + qualifying +
                ", \nsprintQualifying=" + sprintQualifying +
                ", \nsprint=" + sprint +
                ", \nresults=" + results +
                '}';
    }
}
