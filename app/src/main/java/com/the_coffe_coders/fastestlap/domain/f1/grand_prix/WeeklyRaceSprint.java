package com.the_coffe_coders.fastestlap.domain.f1.grand_prix;

import androidx.room.Entity;
import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Entity(tableName = "WeeklyRaceSprint")
public class WeeklyRaceSprint extends WeeklyRace {
    private SprintQualifying sprintQualifying;
    private Sprint sprint;

    @Ignore
    public WeeklyRaceSprint(SprintQualifying sprintQualifying, Sprint sprint) {
        this.sprintQualifying = sprintQualifying;
        this.sprint = sprint;
    }

    public WeeklyRaceSprint() {
    }

    public List<Session> getSessions() {
        List<Session> sessions = new ArrayList<>();
        if (this.firstPractice != null) {
            this.firstPractice.setNumber(1);
        }
        sessions.add(firstPractice);
        sessions.add(sprintQualifying);
        sessions.add(sprint);
        sessions.add(this.getQualifying());
        sessions.add(this.getFinalRace());

        setSessions(sessions);

        return sessions;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
