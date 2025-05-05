package com.the_coffe_coders.fastestlap.mapper;

import android.util.Log;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Qualifying;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Sprint;
import com.the_coffe_coders.fastestlap.domain.grand_prix.SprintQualifying;
import com.the_coffe_coders.fastestlap.dto.PracticeDTO;
import com.the_coffe_coders.fastestlap.dto.QualifyingDTO;
import com.the_coffe_coders.fastestlap.dto.ResultDTO;
import com.the_coffe_coders.fastestlap.dto.SprintDTO;
import com.the_coffe_coders.fastestlap.dto.SprintQualifyingDTO;

public class SessionMapper {
    public static Practice toPractice(PracticeDTO practiceDTO, int number) {
        if (practiceDTO != null) {
            return new Practice(practiceDTO.getDate(), practiceDTO.getTime(), number);
        } else {
            return new Practice();
        }

    }

    public static Qualifying toQualifying(QualifyingDTO qualifyingDTO) {
        return new Qualifying(qualifyingDTO.getDate(), qualifyingDTO.getTime());
    }

    public static SprintQualifying toSprintQualifying(SprintQualifyingDTO qualifyingDTO) {
        return new SprintQualifying(qualifyingDTO.getDate(), qualifyingDTO.getTime());
    }

    public static Sprint toSprint(SprintDTO sprintDTO) {
        return new Sprint(sprintDTO.getDate(), sprintDTO.getTime());
    }


    public static RaceResult toResult(ResultDTO resultDTO) {
        RaceResult raceResult = new RaceResult();
        raceResult.setPosition(resultDTO.getPosition());
        raceResult.setPoints(resultDTO.getPoints());
        raceResult.setConstructor(ConstructorMapper.toConstructor(resultDTO.getConstructor()));
        raceResult.setDriver(DriverMapper.toDriver(resultDTO.getDriver()));
        raceResult.setLaps(resultDTO.getLaps());
        raceResult.setGrid(resultDTO.getGrid());
        raceResult.setStatus(resultDTO.getStatus());
        raceResult.setPoints(resultDTO.getPoints());
        raceResult.setTime(ResultTimeMapper.toResultTime(resultDTO.getTime()));
        raceResult.setFastestLap(ResultFastestLapMapper.toResultFastestLap(resultDTO.getFastestLap()));
        return raceResult;
    }
}
