package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConstructorTableDTO {
    String constructorId;
    String season;
    @SerializedName("Constructors")
    List<ConstructorDTO> constructorDTOList;

    public ConstructorTableDTO(String constructorId, String season, List<ConstructorDTO> constructorDTOList) {
        this.constructorId = constructorId;
        this.season = season;
        this.constructorDTOList = constructorDTOList;
    }

    public String getConstructorId() {
        return constructorId;
    }

    public void setConstructorId(String constructorId) {
        this.constructorId = constructorId;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public List<ConstructorDTO> getConstructorDTOList() {
        return constructorDTOList;
    }

    public void setConstructorDTOList(List<ConstructorDTO> constructorDTOList) {
        this.constructorDTOList = constructorDTOList;
    }
}
