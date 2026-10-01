package com.the_coffe_coders.fastestlap.domain.f1.livetiming;

import androidx.annotation.NonNull;

import lombok.Getter;

/**
 * Domain model representing a single Team Radio message from the OpenF1 API.
 * Fields map directly to the JSON object returned by:
 * https://api.openf1.org/v1/team_radio?session_key=latest
 */
@Getter
public class TeamRadioMessage {

    private final int meetingKey;
    private final int sessionKey;
    private final int driverNumber;
    private final String date;
    private final String recordingUrl;

    public TeamRadioMessage(int meetingKey,
                            int sessionKey,
                            int driverNumber,
                            String date,
                            String recordingUrl) {
        this.meetingKey = meetingKey;
        this.sessionKey = sessionKey;
        this.driverNumber = driverNumber;
        this.date = date;
        this.recordingUrl = recordingUrl;
    }

    @NonNull
    @Override
    public String toString() {
        return "TeamRadioMessage{" +
                "sessionKey=" + sessionKey +
                ", driverNumber=" + driverNumber +
                ", date='" + date + '\'' +
                ", recordingUrl='" + recordingUrl + '\'' +
                '}';
    }
}
