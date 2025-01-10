package com.the_coffe_coders.fastestlap.api;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.dto.DriverStandingsTableDTO;

public class DriversAPIResponse extends APIResponse {
    @SerializedName("StandingsTable")
    private DriverStandingsTableDTO DriverStandingsTableDTO;

    public DriversAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, DriverStandingsTableDTO DriverStandingsTableDTO) {
        super(xmlns, series, url, limit, offset, total);
        this.DriverStandingsTableDTO = DriverStandingsTableDTO;
    }

    public DriverStandingsTableDTO getStandingsTable() {
        return DriverStandingsTableDTO;
    }

    public void setStandingsTable(DriverStandingsTableDTO driverStandingsTableDTO) {
        DriverStandingsTableDTO = driverStandingsTableDTO;
    }

    @Override
    public String toString() {
        return "StandingsAPIResponse{" + super.toString() + "StandingsTable=" + DriverStandingsTableDTO + "}";
    }
}
