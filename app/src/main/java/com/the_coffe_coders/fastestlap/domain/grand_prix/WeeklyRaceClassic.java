package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

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

    public String getDateInterval() {
        String fullDate;
        String startingDate = this.firstPractice.getStartDateTime().toLocalDate().toString();
        String endingDate = startingDate;//TODO fix

        LocalDate startDate = LocalDate.parse(startingDate);
        LocalDate endDate = LocalDate.parse(endingDate);

        if (startDate.getMonth() != endDate.getMonth()) {
            fullDate = startDate.getDayOfMonth() + " " + startDate.getMonth() + " - " + endDate.getDayOfMonth() + " " + endDate.getMonth();
        } else {
            fullDate = startDate.getDayOfMonth() + " - " + endDate.getDayOfMonth() + " " + startDate.getMonth();
        }

        return fullDate;
    }

    @Override
    public List<Session> getSessions() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "WeeklyRaceClassic{" +
                "secondPractice=" + secondPractice +
                ", thirdPractice=" + thirdPractice +
                '}';
    }
}
