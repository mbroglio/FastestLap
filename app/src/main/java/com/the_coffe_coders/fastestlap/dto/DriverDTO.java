package com.the_coffe_coders.fastestlap.dto;

import androidx.room.Ignore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class DriverDTO {
    private String driverId;
    private String permanentNumber;
    private String code;
    @Ignore
    private String url;
    private String givenName;
    private String familyName;
    private String dateOfBirth;
    @Ignore
    private String nationality;
}
