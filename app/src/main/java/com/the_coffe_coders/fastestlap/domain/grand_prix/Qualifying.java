package com.the_coffe_coders.fastestlap.domain.grand_prix;

import org.threeten.bp.ZonedDateTime;

public class Qualifying extends Session{

    public Qualifying(String sessionId, Boolean isFinished, Boolean isUnderway, ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
        super(sessionId, isFinished, isUnderway, startDateTime, endDateTime);
    }
    public Qualifying() {

    }
}
