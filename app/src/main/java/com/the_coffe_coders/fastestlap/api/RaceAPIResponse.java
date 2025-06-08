package com.the_coffe_coders.fastestlap.api;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.dto.RaceTableDTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
public class RaceAPIResponse extends APIResponse {

    @SerializedName("RaceTable")
    private RaceTableDTO RaceTableDTO;

    public RaceAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, RaceTableDTO raceTableDTO) {
        super(xmlns, series, url, limit, offset, total);
        this.RaceTableDTO = raceTableDTO;
    }

    public RaceTableDTO getRaceTable() {
        return RaceTableDTO;
    }

}
