package com.the_coffe_coders.fastestlap.domain.api;

import com.the_coffe_coders.fastestlap.dto.RaceTableDTO;

public class RaceAPIResponse extends APIResponse {
    private RaceTableDTO RaceTableDTO;

    public RaceAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, RaceTableDTO raceTableDTO) {
        super(xmlns, series, url, limit, offset, total);
        this.RaceTableDTO = raceTableDTO;
    }
    public RaceTableDTO getRaceTable() {
        return RaceTableDTO;
    }
    public void setRaceTable(RaceTableDTO raceTableDTO) {
        this.RaceTableDTO = raceTableDTO;
    }
}
