package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.threeten.bp.LocalDateTime;

import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        WeeklyRace that = (WeeklyRace) o;
        return Objects.equals(round, that.round);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, round);
    }

    public boolean isWeekFinished() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(this.getFinalRace().getEndDateTime());
    }

    public void setTrack(Track track) {
        if(this.track == null) {
            this.track = track;
            return;
        }

        String trackId = this.track.getTrackId();
        this.track = track;
        this.track.setTrackId(trackId);
    }
}
