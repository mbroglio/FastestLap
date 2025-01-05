package com.the_coffe_coders.fastestlap.api;


import com.the_coffe_coders.fastestlap.dto.RaceTableDTO;

public class RaceResultsAPIResponse extends APIResponse{
    private RaceTableDTO RaceTable;

    public RaceResultsAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, RaceTableDTO RaceTable) {
        super(xmlns, series, url, limit, offset, total);
        this.RaceTable = RaceTable;
    }

    public RaceTableDTO getRaceTable() {
        return RaceTable;
    }

    public void setRaceTable(RaceTableDTO RaceTable) {
        this.RaceTable = RaceTable;
    }

}
