package com.the_coffe_coders.fastestlap.dto;

public class SprintQualifyingDTO {
    private String date;
    private String time;

    public SprintQualifyingDTO(String date, String time) {
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
