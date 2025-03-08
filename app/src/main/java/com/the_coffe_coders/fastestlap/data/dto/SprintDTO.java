package com.the_coffe_coders.fastestlap.data.dto;

public class SprintDTO {

    private String date;
    private String time;

    public SprintDTO(String date, String time) {
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
