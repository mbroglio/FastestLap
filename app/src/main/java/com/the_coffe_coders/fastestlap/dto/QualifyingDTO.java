package com.the_coffe_coders.fastestlap.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class QualifyingDTO {

    private String date;
    private String time;

    public QualifyingDTO(String date, String time) {
        this.date = date;
        this.time = time;
    }
}

