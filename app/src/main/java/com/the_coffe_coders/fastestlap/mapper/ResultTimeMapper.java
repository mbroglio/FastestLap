package com.the_coffe_coders.fastestlap.mapper;

import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResultTime;
import com.the_coffe_coders.fastestlap.dto.ResultTimeDTO;

public class ResultTimeMapper {
    public static RaceResultTime toResultTime(ResultTimeDTO timeDTO) {
        if(timeDTO != null){
            RaceResultTime resultTime = new RaceResultTime();
            resultTime.setTime(timeDTO.getTime());
            resultTime.setMillis(timeDTO.getMillis());
            return resultTime;
        }else{
            return null;
        }
    }
}
