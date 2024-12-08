package com.the_coffe_coders.fastestlap.domain.grand_prix;

import java.util.List;

import org.threeten.bp.ZonedDateTime;

public class Race extends Session {
    private String season;
    private String round;
    private String url;
    private String raceName;
    private Circuit Circuit;
    public List<Result> Results;

    public Race(String season, String round, String url, String raceName, Circuit circuit, List<Result> results, String sessionId, Boolean isFinished, Boolean isUnderway, ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
        super(sessionId, isFinished, isUnderway, startDateTime, endDateTime);
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        Circuit = circuit;
        Results = results;
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

    public List<Result> getResults() {
        return Results;
    }

    public void setResults(List<Result> results) {
        Results = results;
    }
}
