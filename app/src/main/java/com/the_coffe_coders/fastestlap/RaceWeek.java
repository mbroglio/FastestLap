package com.the_coffe_coders.fastestlap;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

public class RaceWeek {
    private LocalDate startDate;
    private LocalDate endDate;
    private String trackId;
    private String gpName;
    private String eventNumber;

    private String year;

    private Session[] sessions = new Session[5];

    public RaceWeek(JSONObject race, String trackId) throws JSONException {
        this.trackId = trackId;

        this.gpName = race.getString("raceName").toUpperCase();
        this.eventNumber = race.getString("round");
        this.year = race.getString("season");
        setDate(race);
        setSessions(race);
    }

    private void setSessions(JSONObject race) throws JSONException {
        int i = 0;
        for (int j = 0; j < Constants.SESSIONS.length; j++) {
            try {
                JSONObject session = race.getJSONObject(Constants.SESSIONS[j]);
                Log.i("RaceWeek", "Testing Session " + Constants.SESSIONS[j]);
                sessions[i] = new Session(session, Constants.SESSIONS[j]);
                i++;
            } catch (JSONException e) {
                // Handle specific cases
                if (Constants.SESSIONS[j].equals("SprintQualifying") || Constants.SESSIONS[j].equals("Sprint")) {
                    Log.i("RaceWeek", "Testing Session " + Constants.SESSIONS[j]);
                    try {
                        JSONObject session = race.getJSONObject(Constants.SESSIONS[j]);
                        sessions[i] = new Session(session, Constants.SESSIONS[j]);
                        i++;
                    } catch (JSONException ex) {
                        // Session does not exist, continue
                    }
                } else if (Constants.SESSIONS[j].equals("Race")) {
                    sessions[i] = new Session(race, Constants.SESSIONS[j]);
                    i++;
                }
            }
        }
    }

    public void setDate(JSONObject race) throws JSONException {
        String firstDate = race.getJSONObject("FirstPractice").getString("date");
        String lastDate = race.getString("date");

        this.startDate = LocalDate.parse(firstDate);
        this.endDate = LocalDate.parse(lastDate);
    }

    public String getDate() {
        if (startDate.getMonth() != endDate.getMonth()) {
            return startDate.getDayOfMonth() + " " + startDate.getMonth() + " - " + endDate.getDayOfMonth() + " " + endDate.getMonth();
        } else {
            return startDate.getDayOfMonth() + " - " + endDate.getDayOfMonth() + " " + startDate.getMonth();
        }
    }

    public Session getNextSession() {
        for (Session session : sessions) {
            if (!session.isFinished() && !session.isUnderway()) {
                return session;
            }
        }
        return null;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getGpName() {
        return gpName;
    }

    public void setGpName(String gpName) {
        this.gpName = gpName;
    }

    public String getEventNumber() {
        return eventNumber;
    }

    public void setEventNumber(String eventNumber) {
        this.eventNumber = eventNumber;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Session[] getSessions() {
        return sessions;
    }

    public void setSessions(Session[] sessions) {
        this.sessions = sessions;
    }
}
