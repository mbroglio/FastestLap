package com.the_coffe_coders.fastestlap.domain.race_week;

public class ThirdPractice {
    private String date;
    private String time;

    public ThirdPractice(String date, String time) {
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
