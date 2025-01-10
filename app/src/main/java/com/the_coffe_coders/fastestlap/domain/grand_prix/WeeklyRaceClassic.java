package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "WeeklyRaceClassic")
public class WeeklyRaceClassic extends WeeklyRace {
    private Practice secondPractice;
    private Practice thirdPractice;

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

        setSessions(sessions);

        return sessions;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + "WeeklyRaceClassic{" +
                "secondPractice=" + secondPractice +
                ", thirdPractice=" + thirdPractice +
                '}';
    }
}
