package com.the_coffe_coders.fastestlap.domain.grand_prix;

import org.threeten.bp.ZonedDateTime;

public class Sprint extends Session{
    public Sprint(String date, String time) {
        super(date, time);
        setEndDateTime();
    }

    public Sprint() {

    }
}
