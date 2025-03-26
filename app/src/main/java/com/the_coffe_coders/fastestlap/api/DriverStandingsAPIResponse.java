package com.the_coffe_coders.fastestlap.api;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.dto.DriverStandingsTableDTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
public class DriverStandingsAPIResponse extends APIResponse {
    @SerializedName("StandingsTable")
    private DriverStandingsTableDTO DriverStandingsTableDTO;

    public DriverStandingsAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, DriverStandingsTableDTO DriverStandingsTableDTO) {
        super(xmlns, series, url, limit, offset, total);
        this.DriverStandingsTableDTO = DriverStandingsTableDTO;
    }

    public DriverStandingsTableDTO getStandingsTable() {
        return DriverStandingsTableDTO;
    }
}
