package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
//@EqualsAndHashCode(callSuper = true)
@ToString
@NoArgsConstructor
@Entity(tableName = "WeeklyRaceClassic")
public class WeeklyRaceClassic extends WeeklyRace {
    private Practice secondPractice;
    private Practice thirdPractice;

    @Override
    public List<Session> getSessions() {
        List<Session> sessions = new ArrayList<>();
        sessions.add(this.firstPractice);
        sessions.add(this.secondPractice);
        sessions.add(this.thirdPractice);
        sessions.add(this.getQualifying());
        sessions.add(this.getFinalRace());

        setSessions(sessions);

        return sessions;
    }
}
