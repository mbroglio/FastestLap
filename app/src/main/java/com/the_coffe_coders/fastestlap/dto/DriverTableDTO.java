package com.the_coffe_coders.fastestlap.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DriverTableDTO {
    String driverId;
    String season;
    @SerializedName("Drivers")
    List<DriverDTO> driverDTOList;

    public DriverTableDTO(String driverId, String season, List<DriverDTO> driverDTOList) {
        this.driverId = driverId;
        this.season = season;
        this.driverDTOList = driverDTOList;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public List<DriverDTO> getDriverDTOList() {
        return driverDTOList;
    }

    public void setDriverDTOList(List<DriverDTO> driverDTOList) {
        this.driverDTOList = driverDTOList;
    }
}
