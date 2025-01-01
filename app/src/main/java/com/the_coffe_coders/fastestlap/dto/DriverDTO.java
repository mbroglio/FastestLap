package com.the_coffe_coders.fastestlap.dto;

public class DriverDTO {

    private String driverId;
    private String permanentNumber;
    private String code;
    private String url;
    private String givenName;
    private String familyName;
    private String dateOfBirth;
    private String nationality;

    public DriverDTO(String driverId, String permanentNumber, String code, String url, String givenName, String familyName, String dateOfBirth, String nationality) {
        this.driverId = driverId;
        this.permanentNumber = permanentNumber;
        this.code = code;
        this.url = url;
        this.givenName = givenName;
        this.familyName = familyName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getPermanentNumber() {
        return permanentNumber;
    }

    public String getCode() {
        return code;
    }

    public String getUrl() {
        return url;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public void setPermanentNumber(String permanentNumber) {
        this.permanentNumber = permanentNumber;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    @Override
    public String toString() {
        return "DriverDTO{" +
                "driverId='" + driverId + '\'' +
                ", permanentNumber='" + permanentNumber + '\'' +
                ", code='" + code + '\'' +
                ", url='" + url + '\'' +
                ", givenName='" + givenName + '\'' +
                ", familyName='" + familyName + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", nationality='" + nationality + '\'' +
                '}';
    }
}
