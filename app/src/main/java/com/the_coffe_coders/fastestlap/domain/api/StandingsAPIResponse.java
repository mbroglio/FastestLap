package com.the_coffe_coders.fastestlap.domain.api;

import com.the_coffe_coders.fastestlap.domain.driver.StandingsTable;

public class StandingsAPIResponse extends APIResponse{
    private StandingsTable StandingsTable;

    public StandingsAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, StandingsTable StandingsTable) {
        super(xmlns, series, url, limit, offset, total);
        this.StandingsTable = StandingsTable;
    }

    public StandingsTable getStandingsTable() {
        return StandingsTable;
    }

    public void setStandingsTable(StandingsTable standingsTable) {
        StandingsTable = standingsTable;
    }

    @Override
    public String toString() {
        return "StandingsAPIResponse{" + super.toString() + "StandingsTable=" + StandingsTable + "}" ;
    }
}
