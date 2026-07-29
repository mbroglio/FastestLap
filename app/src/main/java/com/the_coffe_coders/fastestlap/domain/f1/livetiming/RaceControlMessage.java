package com.the_coffe_coders.fastestlap.domain.f1.livetiming;

import androidx.annotation.NonNull;

import lombok.Getter;

/**
 * Domain model representing a single Race Control message from the OpenF1 API.
 * Fields map directly to the JSON object returned by:
 * https://api.openf1.org/v1/race_control?session_key=latest
 */
@Getter
public class RaceControlMessage {

    private final int meetingKey;
    private final int sessionKey;
    private final String date;
    private final Integer driverNumber; // nullable
    private final Integer lapNumber;    // nullable
    private final String category;
    private final String flag;          // nullable
    private final String scope;         // nullable
    private final Integer sector;       // nullable
    private final String qualifyingPhase; // nullable
    private final String message;

    public RaceControlMessage(int meetingKey,
                              int sessionKey,
                              String date,
                              Integer driverNumber,
                              Integer lapNumber,
                              String category,
                              String flag,
                              String scope,
                              Integer sector,
                              String qualifyingPhase,
                              String message) {
        this.meetingKey = meetingKey;
        this.sessionKey = sessionKey;
        this.date = date;
        this.driverNumber = driverNumber;
        this.lapNumber = lapNumber;
        this.category = category;
        this.flag = flag;
        this.scope = scope;
        this.sector = sector;
        this.qualifyingPhase = qualifyingPhase;
        this.message = message;
    }

    @NonNull
    @Override
    public String toString() {
        return "RaceControlMessage{" +
                "sessionKey=" + sessionKey +
                ", date='" + date + '\'' +
                ", category='" + category + '\'' +
                ", flag='" + flag + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
