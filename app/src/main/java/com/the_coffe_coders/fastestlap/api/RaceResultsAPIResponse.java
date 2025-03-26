package com.the_coffe_coders.fastestlap.api;


import com.the_coffe_coders.fastestlap.dto.RaceDTO;
import com.the_coffe_coders.fastestlap.dto.RaceTableDTO;
import com.the_coffe_coders.fastestlap.dto.ResultDTO;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
public class RaceResultsAPIResponse extends APIResponse {
    private RaceTableDTO RaceTable;

    public RaceResultsAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, RaceTableDTO RaceTable) {
        super(xmlns, series, url, limit, offset, total);
        this.RaceTable = RaceTable;
    }


    public RaceDTO getFinalRace() {
        return RaceTable.getRace();
    }


    public List<ResultDTO> getRaceResults() {
        return RaceTable.getRace().getResults();
    }

}
