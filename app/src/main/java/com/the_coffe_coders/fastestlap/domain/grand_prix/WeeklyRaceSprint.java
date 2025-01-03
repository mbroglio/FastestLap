package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "WeeklyRaceSprint")
public class WeeklyRaceSprint extends WeeklyRace {
    private Qualifying sprintQualifying;
    private Sprint sprint;

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

        setSessions(sessions);

        return sessions;
    }

    @NonNull
    @Override
    public String toString() {
        return "WeeklyRaceSprint{" + "sprintQualifying=" + sprintQualifying + ", sprint=" + sprint + '}';
    }
}
