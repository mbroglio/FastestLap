package com.the_coffe_coders.fastestlap.domain.f1.grand_prix;

public class Qualifying extends Session {
    public Qualifying(String date, String time) {
        super(date, time);
        setEndDateTime();
    }
}
