package com.the_coffe_coders.fastestlap.domain.grand_prix;

import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

public class Race extends Session {
    public List<RaceResult> raceResults;
    public LocalDateTime dateTime;//TODO REMOVE ?
    private String season;
    private String round;
    private String url;
    private String raceName;
    private Circuit Circuit;

    public Race(String season, String round, String url, String raceName, Circuit circuit, List<RaceResult> raceResults, String sessionId, Boolean isFinished, Boolean isUnderway, String date, String time) {
        super(date, time);
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        this.Circuit = circuit;
        this.raceResults = raceResults;
        setEndDateTime();
    }

    public Race() {
        raceResults = new ArrayList<>();
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
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

    public int getRoundAsInt() {
        return Integer.parseInt(round);
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

    public List<RaceResult> getResults() {
        return raceResults;
    }

    public void setResults(List<RaceResult> raceResults) {
        this.raceResults = raceResults;
    }

    public void addResult(RaceResult result) {
        this.raceResults.add(result);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (RaceResult raceResult : raceResults) {
            sb.append(raceResult.toString());
        }
        return "Race{" +
                "raceResults=" + sb +
                ", dateTime=" + dateTime +
                ", season='" + season + '\'' +
                ", round='" + round + '\'' +
                ", url='" + url + '\'' +
                ", raceName='" + raceName + '\'' +
                ", Circuit=" + Circuit +
                '}';
    }
}
