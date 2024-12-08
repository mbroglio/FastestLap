package com.the_coffe_coders.fastestlap.domain.grand_prix;

import org.threeten.bp.ZonedDateTime;

public class Practice extends Session{
    private int number;

    public Practice(String sessionId, Boolean isFinished, Boolean isUnderway, ZonedDateTime startDateTime, ZonedDateTime endDateTime, int number) {
        super(sessionId, isFinished, isUnderway, startDateTime, endDateTime);
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getPractice() {
        return "Practice " + number + " \n" + super.toString();
    }
}
