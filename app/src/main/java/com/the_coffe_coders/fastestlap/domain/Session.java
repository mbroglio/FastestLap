package com.the_coffe_coders.fastestlap.domain;

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

    public Session(JSONObject session, String sessionId) throws JSONException {
        this.sessionId = sessionId;
        this.isFinished = false;
        this.isUnderway = false;

        setTime(session, sessionId);
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

    public void setTime(JSONObject session, String sessionId) throws JSONException {
        this.startDateTime = ZonedDateTime
                .parse(session.getString("date") + "T" + session.getString("time"))
                .withZoneSameInstant(ZoneId.of("UTC"));

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
