package com.the_coffe_coders.fastestlap.domain.race_week;

public class Races {
    private String season;
    private String round;
    private String url;
    private String raceName;
    private Circuit Circuit;
    private String date;
    private String time;
    private FirstPractice FirstPractice;
    private SecondPractice SecondPractice;
    private ThirdPractice ThirdPractice;
    private Qualifying Qualifying;
    private SprintQualifying SprintQualifying;
    private Sprint Sprint;

    public Races(String season, String round, String url, String raceName, Circuit circuit, String date, String time, FirstPractice firstPractice, SecondPractice secondPractice, ThirdPractice thirdPractice, Qualifying qualifying, SprintQualifying sprintQualifying, Sprint sprint) {
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        Circuit = circuit;
        this.date = date;
        this.time = time;
        FirstPractice = firstPractice;
        SecondPractice = secondPractice;
        ThirdPractice = thirdPractice;
        Qualifying = qualifying;
        SprintQualifying = sprintQualifying;
        Sprint = sprint;
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

    public Circuit getCircuit() {
        return Circuit;
    }

    public void setCircuit(Circuit circuit) {
        Circuit = circuit;
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

    public FirstPractice getFirstPractice() {
        return FirstPractice;
    }

    public void setFirstPractice(FirstPractice firstPractice) {
        FirstPractice = firstPractice;
    }

    public SecondPractice getSecondPractice() {
        return SecondPractice;
    }

    public void setSecondPractice(SecondPractice secondPractice) {
        SecondPractice = secondPractice;
    }

    public ThirdPractice getThirdPractice() {
        return ThirdPractice;
    }

    public void setThirdPractice(ThirdPractice thirdPractice) {
        ThirdPractice = thirdPractice;
    }

    public Qualifying getQualifying() {
        return Qualifying;
    }

    public void setQualifying(Qualifying qualifying) {
        Qualifying = qualifying;
    }

    public SprintQualifying getSprintQualifying() {
        return SprintQualifying;
    }

    public void setSprintQualifying(SprintQualifying sprintQualifying) {
        SprintQualifying = sprintQualifying;
    }

    public Sprint getSprint() {
        return Sprint;
    }

    public void setSprint(Sprint sprint) {
        Sprint = sprint;
    }
}
