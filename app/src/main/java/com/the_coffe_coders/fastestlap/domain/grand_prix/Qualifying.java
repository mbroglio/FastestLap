package com.the_coffe_coders.fastestlap.domain.grand_prix;

import com.the_coffe_coders.fastestlap.util.Constants;

import org.threeten.bp.LocalDateTime;

public class Qualifying extends Session{
    public Qualifying(String date, String time) {
        super(date, time);
        setEndDateTime();
    }
}
