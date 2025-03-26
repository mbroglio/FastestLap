package com.the_coffe_coders.fastestlap.api;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.dto.DriverTableDTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
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

}
