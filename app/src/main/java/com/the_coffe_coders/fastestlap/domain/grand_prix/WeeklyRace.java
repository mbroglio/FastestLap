package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.threeten.bp.LocalDateTime;

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
    protected Practice firstPractice;
    private Qualifying Qualifying;
    private Race finalRace;

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

    public LocalDateTime getDateTime() {
        return finalRace.getStartDateTime();
    }

    public String getDateInterval() {
        String fullDate;

        LocalDateTime startDate = this.firstPractice.getStartDateTime();
        LocalDateTime endDate = this.getFinalRace().getStartDateTime();//Inizio al posto della fine

        if (startDate.getMonth() != endDate.getMonth()) {
            fullDate = startDate.getDayOfMonth() + " " + startDate.getMonth() + " - " + endDate.getDayOfMonth() + " " + endDate.getMonth();
        } else {
            fullDate = startDate.getDayOfMonth() + " - " + endDate.getDayOfMonth() + " " + startDate.getMonth();
        }

        return fullDate;
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

    public abstract List<Session> getSessions();

    public Session findNextEvent(List<Session> sessions) {
        Session nextSession = null;

        setSessions(sessions);

        for (Session session : sessions) {
            if (!session.isFinished()) {
                nextSession = session;
                break;
            }
        }

        return nextSession;
    }

    public void setSessions(List<Session> sessions) {
        LocalDateTime currentDateTime = LocalDateTime.now();

        for (Session session : sessions) {
            session.setSessionStatus();
        }
    }

    public boolean isUnderway() {
        for (Session session : this.getSessions()) {
            if (session.isUnderway()) {
                return true;
            }
        }

        return false;
    }

    public boolean isWeekFinished() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(this.getFinalRace().getEndDateTime());
    }

    public Practice getFirstPractice() {
        return this.firstPractice;
    }
}
