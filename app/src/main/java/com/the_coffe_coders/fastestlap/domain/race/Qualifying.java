package com.the_coffe_coders.fastestlap.domain.race;

public class Qualifying {
    private String date;
    private String time;

    public Qualifying(String date, String time) {
        this.date = date;
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
