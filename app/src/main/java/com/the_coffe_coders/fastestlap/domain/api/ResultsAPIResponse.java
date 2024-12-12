package com.the_coffe_coders.fastestlap.domain.api;

import com.the_coffe_coders.fastestlap.domain.race_result.RaceTable;

public class ResultsAPIResponse extends APIResponse{
    private RaceTable RaceTable;

    public ResultsAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, RaceTable RaceTable) {
        super(xmlns, series, url, limit, offset, total);
        this.RaceTable = RaceTable;
    }

    public RaceTable getRaceTable() {
        return RaceTable;
    }

    public void setRaceTable(RaceTable RaceTable) {
        this.RaceTable = RaceTable;
    }
}
