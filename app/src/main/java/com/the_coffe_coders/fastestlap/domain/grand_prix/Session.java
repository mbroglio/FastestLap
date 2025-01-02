package com.the_coffe_coders.fastestlap.domain.grand_prix;

import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import com.the_coffe_coders.fastestlap.util.Constants;

public class Session {
    private String sessionId;
    private Boolean isFinished;
    private Boolean isUnderway;
    private ZonedDateTime startDateTime;
    private ZonedDateTime endDateTime;

    public Session(String sessionId, Boolean isFinished, Boolean isUnderway, String date, String time) {
        this.sessionId = sessionId;
        this.isFinished = isFinished;
        this.isUnderway = isUnderway;

        setStartDateTime(date, time);
        setEndDateTime();
    }

    public Session() {

    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Boolean isFinished() {
        return isFinished;
    }

    public void setFinished(Boolean finished) {
        isFinished = finished;
    }

    public Boolean isUnderway() {
        return isUnderway;
    }

    public void setUnderway(Boolean underway) {
        isUnderway = underway;
    }

    public ZonedDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(ZonedDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }
    public void setStartDateTime(String date, String time) {
        this.startDateTime = ZonedDateTime.parse(
                date + "T" + time + "[UTC]"
        ).withZoneSameInstant(ZoneId.systemDefault());
    }

    public ZonedDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setStartDateTime() {
        this.startDateTime = ZonedDateTime.now();
    }

    public void setEndDateTime(ZonedDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public void setEndDateTime() {
        this.endDateTime = this.startDateTime.plusMinutes(Constants.SESSION_DURATION.get(sessionId));
    }

    public String getTime() {
        String start = startDateTime.toLocalTime().toString();
        String end = endDateTime.toLocalTime().toString();

        return start + " - " + end;
    }

    public String getStartingTime() {
        return getStartDateTime().toLocalTime().toString();
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", isFinished=" + isFinished +
                ", isUnderway=" + isUnderway +
                ", startDateTime=" + startDateTime +
                ", endDateTime=" + endDateTime +
                '}';
    }
}
