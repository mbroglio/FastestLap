package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.List;
@Entity(tableName = "WeeklyRace")
public abstract class WeeklyRace {
    @PrimaryKey(autoGenerate = true)
    private int uid;
    private String season;
    private String round;
    private String url;
    private String raceName;
    private Circuit Circuit;
    private ZonedDateTime dateTime;
    protected Practice firstPractice;
    private Qualifying Qualifying;
    private Race finalRace;
    //private Sprint Sprint;

    protected WeeklyRace(String season, String round, String url, String raceName, Circuit circuit, String date, String time, Qualifying qualifying, Race finalRace, Practice firstPractice) {
        this.season = season;
        this.round = round;
        this.url = url;
        this.raceName = raceName;
        this.Circuit = circuit;
        this.firstPractice = firstPractice;

        setDateTime(date, time);
    }

    @Ignore
    public WeeklyRace() {

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

    public com.the_coffe_coders.fastestlap.domain.grand_prix.Circuit getCircuit() {
        return Circuit;
    }

    public void setCircuit(com.the_coffe_coders.fastestlap.domain.grand_prix.Circuit circuit) {
        Circuit = circuit;
    }

    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(ZonedDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public void setDateTime(String date, String time) {
        this.dateTime = ZonedDateTime.parse(
                date + "T" + time + "[UTC]"
        ).withZoneSameInstant(ZoneId.systemDefault());
    }

    public void setFirstPractice(Practice firstPractice) {
        this.firstPractice = firstPractice;
    }

    public com.the_coffe_coders.fastestlap.domain.grand_prix.Qualifying getQualifying() {
        return Qualifying;
    }

    public void setQualifying(com.the_coffe_coders.fastestlap.domain.grand_prix.Qualifying qualifying) {
        Qualifying = qualifying;
    }

    public Race getFinalRace() {
        return finalRace;
    }

    public void setFinalRace(Race finalRace) {
        this.finalRace = finalRace;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public abstract String getDateInterval();

    public abstract List<Session> getSessions();

    public Session findNextEvent(List<Session> sessions) {
        if(true)
            return firstPractice;
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.systemDefault());
        Session nextSession = null;
        int i = 0;

        for (Session session : sessions) {
            if (currentDateTime.isAfter(session.getStartDateTime()) && currentDateTime.isBefore(session.getEndDateTime())) {
                session.setUnderway(true);
            } else if (currentDateTime.isAfter(session.getEndDateTime())) {
                session.setUnderway(false);
                session.setFinished(true);
            }
        }

        for (Session session : sessions) {
            if (!session.isFinished()) {
                nextSession = session;
                break;
            }
        }

        return nextSession;
    }

    public boolean isUnderway() {
        for (Session session : this.getSessions()) {
            if (session.isUnderway()) {
                return true;
            }
        }

        return false;
    }

    public Practice getFirstPractice() {
        return this.firstPractice;
    }
}
