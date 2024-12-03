package com.the_coffe_coders.fastestlap.domain.race_week;

public class FirstPractice {
    private String date;
    private String time;

    public FirstPractice(String date, String time) {
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
