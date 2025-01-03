package com.the_coffe_coders.fastestlap.mapper;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Qualifying;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Sprint;
import com.the_coffe_coders.fastestlap.dto.PracticeDTO;
import com.the_coffe_coders.fastestlap.dto.QualifyingDTO;
import com.the_coffe_coders.fastestlap.dto.ResultDTO;
import com.the_coffe_coders.fastestlap.dto.SprintDTO;
import com.the_coffe_coders.fastestlap.dto.SprintQualifyingDTO;

public class SessionMapper {
    public static Practice toPractice(PracticeDTO practiceDTO, int number) {
        return new Practice(practiceDTO.getDate(), practiceDTO.getTime(), number);
    }

    public static Qualifying toQualifying(QualifyingDTO qualifyingDTO) {
        return new Qualifying(qualifyingDTO.getDate(), qualifyingDTO.getTime());
    }

    public static Qualifying toQualifying(SprintQualifyingDTO qualifyingDTO) {
        return new Qualifying(qualifyingDTO.getDate(), qualifyingDTO.getTime());
    }

    public static Sprint toSprint(SprintDTO sprintDTO) {
        return new Sprint(sprintDTO.getDate(), sprintDTO.getTime());
    }

    public static RaceResult toResult(ResultDTO resultDTO) {
        return new RaceResult(resultDTO.getNumber(), resultDTO.getPosition(), resultDTO.getPositionText(), resultDTO.getPoints(), DriverMapper.toDriver(resultDTO.getDriver()), ConstructorMapper.toConstructor(resultDTO.getConstructor()), resultDTO.getGrid(), resultDTO.getLaps(), resultDTO.getStatus());
    }
}
