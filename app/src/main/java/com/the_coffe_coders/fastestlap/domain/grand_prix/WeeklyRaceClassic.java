package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity(tableName = "WeeklyRaceClassic")
public class WeeklyRaceClassic extends WeeklyRace {
    private Practice secondPractice;
    private Practice thirdPractice;

    public WeeklyRaceClassic(String season, String round, String url, String raceName, Circuit circuit, String date, String time, Qualifying qualifying, Race finalRace, Practice firstPractice, Practice secondPractice, Practice thirdPractice) {
        super(season, round, url, raceName, circuit, date, time,qualifying, finalRace, firstPractice);
        this.secondPractice = secondPractice;
        this.thirdPractice = thirdPractice;
    }

    public WeeklyRaceClassic(Practice secondPractice, Practice thirdPractice) {
        this.secondPractice = secondPractice;
        this.thirdPractice = thirdPractice;
    }

    public Practice getSecondPractice() {
        return secondPractice;
    }

    public void setSecondPractice(Practice secondPractice) {
        this.secondPractice = secondPractice;
    }

    public Practice getThirdPractice() {
        return thirdPractice;
    }

    public void setThirdPractice(Practice thirdPractice) {
        this.thirdPractice = thirdPractice;
    }

    @Override
    public List<Session> getSessions() {
        List<Session> sessions = new ArrayList<>();
        sessions.add(this.firstPractice);
        sessions.add(this.secondPractice);
        sessions.add(this.thirdPractice);
        sessions.add(this.getQualifying());
        sessions.add(this.getFinalRace());

        return sessions;
    }

    @Override
    public String toString() {
        return "WeeklyRaceClassic{" +
                "secondPractice=" + secondPractice +
                ", thirdPractice=" + thirdPractice +
                '}';
    }
}
