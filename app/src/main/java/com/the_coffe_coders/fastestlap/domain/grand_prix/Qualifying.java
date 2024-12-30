package com.the_coffe_coders.fastestlap.domain.grand_prix;

public class Qualifying extends Session{

    public Qualifying(String sessionId, Boolean isFinished, Boolean isUnderway, String date, String time) {
        super(sessionId, isFinished, isUnderway, date, time);
    }

    public Qualifying() {

    }
}
