package com.the_coffe_coders.fastestlap.dto;

import android.util.Log;

import com.the_coffe_coders.fastestlap.domain.race.Circuit;
import com.the_coffe_coders.fastestlap.domain.race.FirstPractice;
import com.the_coffe_coders.fastestlap.domain.race.Qualifying;
import com.the_coffe_coders.fastestlap.domain.race.SecondPractice;
import com.the_coffe_coders.fastestlap.domain.race.Sprint;
import com.the_coffe_coders.fastestlap.domain.race.SprintQualifying;
import com.the_coffe_coders.fastestlap.domain.race.ThirdPractice;

import org.threeten.bp.LocalDate;

public class RaceDTO {
    private String season;
    private String round;
    private String url;
    private String raceName;
    private Circuit Circuit;
    private String date;
    private String time;
    private PracticeDTO FirstPractice;
    private PracticeDTO SecondPractice;
    private PracticeDTO ThirdPractice;
    private Qualifying Qualifying;
    private SprintQualifying SprintQualifying;
    private Sprint Sprint;

    public RaceDTO(String season, String round, String url, String raceName, Circuit circuit, String date, String time, PracticeDTO firstPractice, PracticeDTO secondPractice, PracticeDTO thirdPractice, Qualifying qualifying, SprintQualifying sprintQualifying, Sprint sprint) {
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        this.Circuit = circuit;
        this.date = date;
        this.time = time;
        this.FirstPractice = firstPractice;
        this.SecondPractice = secondPractice;
        this.ThirdPractice = thirdPractice;
        this.Qualifying = qualifying;
        this.SprintQualifying = sprintQualifying;
        this.Sprint = sprint;
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

    public Sprint getSprint() {
        return Sprint;
    }

    public void setSprint(Sprint sprint) {
        Sprint = sprint;
    }

    public SprintQualifying getSprintQualifying() {
        return SprintQualifying;
    }

    public void setSprintQualifying(SprintQualifying sprintQualifying) {
        SprintQualifying = sprintQualifying;
    }

    public Qualifying getQualifying() {
        return Qualifying;
    }

    public void setQualifying(Qualifying qualifying) {
        Qualifying = qualifying;
    }

    public PracticeDTO getThirdPractice() {
        return ThirdPractice;
    }

    public void setThirdPractice(PracticeDTO thirdPractice) {
        ThirdPractice = thirdPractice;
    }

    public PracticeDTO getSecondPractice() {
        return SecondPractice;
    }

    public void setSecondPractice(PracticeDTO secondPractice) {
        SecondPractice = secondPractice;
    }

    public PracticeDTO getFirstPractice() {
        return FirstPractice;
    }

    public void setFirstPractice(PracticeDTO firstPractice) {
        FirstPractice = firstPractice;
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

    public Circuit getCircuit() {
        return Circuit;
    }

    public void setCircuit(Circuit circuit) {
        Circuit = circuit;
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
}
