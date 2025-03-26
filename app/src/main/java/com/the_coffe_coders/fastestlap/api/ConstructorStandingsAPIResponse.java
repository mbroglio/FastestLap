package com.the_coffe_coders.fastestlap.api;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.dto.ConstructorStandingsTableDTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
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

}
