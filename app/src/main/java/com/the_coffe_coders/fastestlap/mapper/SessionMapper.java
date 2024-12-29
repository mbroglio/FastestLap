package com.the_coffe_coders.fastestlap.mapper;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Qualifying;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Sprint;
import com.the_coffe_coders.fastestlap.dto.PracticeDTO;
import com.the_coffe_coders.fastestlap.dto.QualifyingDTO;
import com.the_coffe_coders.fastestlap.dto.ResultDTO;
import com.the_coffe_coders.fastestlap.dto.SprintDTO;
import com.the_coffe_coders.fastestlap.dto.SprintQualifyingDTO;

import org.threeten.bp.ZonedDateTime;

public class SessionMapper {
    public static Practice toPractice(PracticeDTO practiceDTO, int number) {
        Practice practice = new Practice();
        practice.setNumber(number);
        practice.setStartDateTime(practiceDTO.getDate(), practiceDTO.getTime());
        return practice;
    }

    public static Qualifying toQualifying(QualifyingDTO qualifyingDTO) {
        Qualifying qualifying = new Qualifying();

        qualifying.setStartDateTime(qualifyingDTO.getDate(), qualifyingDTO.getTime());
        return qualifying;
    }

    public static Qualifying toQualifying(SprintQualifyingDTO qualifyingDTO) {
        Qualifying qualifying = new Qualifying();
        qualifying.setStartDateTime(qualifyingDTO.getDate(), qualifyingDTO.getTime());
        return qualifying;
    }

    public static Sprint toSprint(SprintDTO sprintDTO) {
        Sprint sprint = new Sprint();
        sprint.setStartDateTime(sprintDTO.getDate(), sprintDTO.getTime());
        return sprint;
    }

    public static RaceResult toResult(ResultDTO resultDTO) {
        return new RaceResult(resultDTO.getNumber(), resultDTO.getPosition(), resultDTO.getPositionText(), resultDTO.getPoints(), DriverMapper.toDriver(resultDTO.getDriver()), ConstructorMapper.toConstructor(resultDTO.getConstructor()), resultDTO.getGrid(), resultDTO.getLaps(), resultDTO.getStatus());
    }
}
