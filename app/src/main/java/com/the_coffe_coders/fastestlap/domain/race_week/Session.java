package com.the_coffe_coders.fastestlap.domain.race_week;

import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import com.the_coffe_coders.fastestlap.utils.Constants;

public class Session {
    private String sessionId;
    private Boolean isFinished;
    private Boolean isUnderway;
    private ZonedDateTime startDateTime;
    private ZonedDateTime endDateTime;

    public Session(String sessionId, Boolean isFinished, Boolean isUnderway, ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
        this.sessionId = sessionId;
        this.isFinished = isFinished;
        this.isUnderway = isUnderway;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;

        setTime();
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

    public ZonedDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(ZonedDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public void setTime() {
        startDateTime = startDateTime.withZoneSameInstant(ZoneId.systemDefault());
        this.endDateTime = this.startDateTime.plusMinutes(Constants.SESSION_DURATION.get(sessionId));
    }


    public String getTime() {
        String start = startDateTime.toLocalTime().toString();
        String end = endDateTime.toLocalTime().toString();

        return start + " - " + end;
    }

    public String getStartingTime() {
        return startDateTime.toLocalTime().toString();
    }
}
