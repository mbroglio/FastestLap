package com.the_coffe_coders.fastestlap.dto;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import org.threeten.bp.LocalDate;

public class RaceDTO {
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

    public RaceDTO(String season, String round, String url, String raceName, CircuitDTO circuit, String date, String time, PracticeDTO firstPractice, PracticeDTO secondPractice, PracticeDTO thirdPractice, QualifyingDTO qualifying, SprintQualifyingDTO sprintQualifying, SprintDTO sprint) {
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
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public SprintDTO getSprint() {
        return sprint;
    }

    public void setSprint(SprintDTO sprint) {
        sprint = sprint;
    }

    public SprintQualifyingDTO getSprintQualifying() {
        return sprintQualifying;
    }

    public void setSprintQualifying(SprintQualifyingDTO sprintQualifying) {
        sprintQualifying = sprintQualifying;
    }

    public QualifyingDTO getQualifying() {
        return qualifying;
    }

    public void setQualifying(QualifyingDTO qualifying) {
        qualifying = qualifying;
    }

    public PracticeDTO getThirdPractice() {
        return thirdPractice;
    }

    public void setThirdPractice(PracticeDTO thirdPractice) {
        thirdPractice = thirdPractice;
    }

    public PracticeDTO getSecondPractice() {
        return secondPractice;
    }

    public void setSecondPractice(PracticeDTO secondPractice) {
        secondPractice = secondPractice;
    }

    public PracticeDTO getFirstPractice() {
        return firstPractice;
    }

    public void setFirstPractice(PracticeDTO firstPractice) {
        firstPractice = firstPractice;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public CircuitDTO getCircuit() {
        return circuit;
    }

    public void setCircuit(CircuitDTO circuit) {
        circuit = circuit;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "RaceDTO{" +
                "season='" + season + '\'' +
                ", round='" + round + '\'' +
                ", url='" + url + '\'' +
                ", raceName='" + raceName + '\'' +
                ", circuit=" + circuit +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", firstPractice=" + firstPractice +
                ", secondPractice=" + secondPractice +
                ", thirdPractice=" + thirdPractice +
                ", qualifying=" + qualifying +
                ", sprintQualifying=" + sprintQualifying +
                ", sprint=" + sprint +
                '}';
    }
}
