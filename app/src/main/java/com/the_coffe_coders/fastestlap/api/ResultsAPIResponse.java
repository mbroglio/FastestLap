package com.the_coffe_coders.fastestlap.api;


import com.the_coffe_coders.fastestlap.dto.RaceTableDTO;

public class ResultsAPIResponse extends APIResponse{
    private RaceTableDTO RaceTable;

    public ResultsAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, RaceTableDTO RaceTable) {
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
