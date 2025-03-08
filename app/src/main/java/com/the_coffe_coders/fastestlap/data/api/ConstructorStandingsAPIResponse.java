package com.the_coffe_coders.fastestlap.data.api;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.data.dto.ConstructorStandingsTableDTO;

public class ConstructorStandingsAPIResponse extends APIResponse {
    @SerializedName("StandingsTable")
    private ConstructorStandingsTableDTO ConstructorStandingsTableDTO;

    public ConstructorStandingsAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, ConstructorStandingsTableDTO ConstructorStandingsTableDTO) {
        super(xmlns, series, url, limit, offset, total);
        this.ConstructorStandingsTableDTO = ConstructorStandingsTableDTO;
    }

    public ConstructorStandingsTableDTO getStandingsTable() {
        return ConstructorStandingsTableDTO;
    }

    public void setStandingsTable(ConstructorStandingsTableDTO constructorStandingsTableDTO) {
        ConstructorStandingsTableDTO = constructorStandingsTableDTO;
    }

    @Override
    public String toString() {
        return "StandingsAPIResponse{" + super.toString() + "StandingsTable=" + ConstructorStandingsTableDTO + "}";
    }
}
