package com.the_coffe_coders.fastestlap.data.api;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.data.dto.DriverTableDTO;

public class DriversAPIResponse extends APIResponse {
    @SerializedName("DriverTable")
    private DriverTableDTO driverTableDTO;

    public DriversAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, DriverTableDTO driverTableDTO) {
        super(xmlns, series, url, limit, offset, total);
        this.driverTableDTO = driverTableDTO;
    }

    public DriverTableDTO getStandingsTable() {
        return driverTableDTO;
    }

    public void setStandingsTable(DriverTableDTO driverTableDTO) {
        this.driverTableDTO = driverTableDTO;
    }

    @Override
    public String toString() {
        return "DriversAPIResponse{" + super.toString() + "DriverTable=" + driverTableDTO + "}";
    }
}
