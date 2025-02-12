package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.threeten.bp.LocalDateTime;

import java.util.List;

@Entity(tableName = "WeeklyRace")
public abstract class WeeklyRace {
    protected Practice firstPractice;
    @PrimaryKey(autoGenerate = true)
    private int uid;
    private String season;
    private String round;
    private String url;
    private String raceName;
    private Track track;
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

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
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

    public void setSessions(List<Session> sessions) {
        for (Session session : sessions) {
            if (session != null)
                session.setSessionStatus();
        }
    }

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

    public boolean isUnderway() {
        for (Session session : this.getSessions()) {
            if (session.getSessionStatus().equals(SessionStatus.IN_PROGRESS)) {
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

    public void setFirstPractice(Practice firstPractice) {
        this.firstPractice = firstPractice;
    }

    public void setFinalRaceResults(List<RaceResult> raceResults) {
        this.finalRace.setResults(raceResults);
    }

    @Override
    public String toString() {
        return "WeeklyRace{" +
                "firstPractice=" + firstPractice +
                ", uid=" + uid +
                ", season='" + season + '\'' +
                ", round='" + round + '\'' +
                ", url='" + url + '\'' +
                ", raceName='" + raceName + '\'' +
                ", track=" + track +
                ", Qualifying=" + Qualifying +
                ", finalRace=" + finalRace +
                '}';
    }
}
