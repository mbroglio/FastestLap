package com.the_coffe_coders.fastestlap.mapper;

import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResultFastestLap;
import com.the_coffe_coders.fastestlap.dto.ResultFastestLapDTO;

public class ResultFastestLapMapper {
    public static RaceResultFastestLap toResultFastestLap(ResultFastestLapDTO fastestLapDTO) {
        if (fastestLapDTO != null) {
            RaceResultFastestLap resultFastestLap = new RaceResultFastestLap();
            resultFastestLap.setRank(fastestLapDTO.getRank());
            resultFastestLap.setLap(fastestLapDTO.getLap());
            resultFastestLap.setTime(ResultTimeMapper.toResultTime(fastestLapDTO.getTime()));
            return resultFastestLap;
        } else {
            return null;
        }
    }
}
