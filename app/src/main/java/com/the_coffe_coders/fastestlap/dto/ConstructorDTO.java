package com.the_coffe_coders.fastestlap.dto;

public class ConstructorDTO {

    private String constructorId;
    private String url;
    private String name;
    private String nationality;

    public ConstructorDTO(String constructorId, String url, String name, String nationality) {
        this.constructorId = constructorId;
        this.url = url;
        this.name = name;
        this.nationality = nationality;
    }

    public String getConstructorId(){
        return this.constructorId;
    }

    public String getUrl(){
        return this.url;
    }

    public String getName(){
        return this.name;
    }

    public String getNationality(){
        return this.nationality;
    }

    public void setConstructorId(String constructorId) {
        this.constructorId = constructorId;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }
}
