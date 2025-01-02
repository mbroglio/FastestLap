package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;
import androidx.room.Ignore;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "WeeklyRaceSprint")
public class WeeklyRaceSprint extends WeeklyRace {
    private Qualifying sprintQualifying;
    private Sprint sprint;

    public WeeklyRaceSprint(String season, String round, String url, String raceName, Circuit circuit, String date, String time, Qualifying qualifying, Race finalRace, Qualifying sprintQualifying, Sprint sprint, Practice firstPractice) {
        super(season, round, url, raceName, circuit, date, time, qualifying, finalRace, firstPractice);
        this.sprintQualifying = sprintQualifying;
        this.sprint = sprint;
    }

    @Ignore
    public WeeklyRaceSprint(Qualifying sprintQualifying, Sprint sprint) {
        this.sprintQualifying = sprintQualifying;
        this.sprint = sprint;
    }

    public WeeklyRaceSprint() {

    }

    public Qualifying getSprintQualifying() {
        return sprintQualifying;
    }

    public void setSprintQualifying(Qualifying sprintQualifying) {
        this.sprintQualifying = sprintQualifying;
    }

    public Sprint getSprint() {
        return sprint;
    }
    public void setSprint(Sprint sprint) {
        this.sprint = sprint;
    }

    public List<Session> getSessions() {
        List<Session> sessions = new ArrayList<>();

        sessions.add(firstPractice);
        sessions.add(sprintQualifying);
        sessions.add(sprint);
        sessions.add(this.getQualifying());
        sessions.add(this.getFinalRace());

        return sessions;
    }

    public void addSession() {

    }

    @Override
    public String toString() {
        return "WeeklyRaceSprint{" +
                "sprintQualifying=" + sprintQualifying +
                ", sprint=" + sprint +
                '}';
    }
}
