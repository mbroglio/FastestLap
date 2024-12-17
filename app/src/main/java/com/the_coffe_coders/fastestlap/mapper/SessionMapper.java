package com.the_coffe_coders.fastestlap.mapper;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Qualifying;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Sprint;
import com.the_coffe_coders.fastestlap.dto.PracticeDTO;
import com.the_coffe_coders.fastestlap.dto.QualifyingDTO;
import com.the_coffe_coders.fastestlap.dto.SprintDTO;
import com.the_coffe_coders.fastestlap.dto.SprintQualifyingDTO;

import org.threeten.bp.ZonedDateTime;

public class SessionMapper {
    public static Practice toPractice(PracticeDTO practiceDTO, int number) {
        Practice practice = new Practice();
        practice.setNumber(number);
        //practice.setStartDateTime(ZonedDateTime.parse(practiceDTO.getDate() + "T" + practiceDTO.getTime() + ":00.000Z"));
        //practice.setDate(practiceDTO.getDate());
        //practice.setTime(practiceDTO.getTime());
        return practice;
    }

    public static Qualifying toQualifying(QualifyingDTO qualifyingDTO) {
        Qualifying qualifying = new Qualifying();

        //qualifying.setStartDateTime(ZonedDateTime.parse(qualifyingDTO.getDate() + "T" + qualifyingDTO.getTime() + ":00.000Z"));
        //practice.setDate(practiceDTO.getDate());
        //practice.setTime(practiceDTO.getTime());
        return qualifying;
    }

    public static Qualifying toQualifying(SprintQualifyingDTO qualifyingDTO) {
        Qualifying qualifying = new Qualifying();

        //qualifying.setStartDateTime(ZonedDateTime.parse(qualifyingDTO.getDate() + "T" + qualifyingDTO.getTime() + ":00.000Z"));
        //practice.setDate(practiceDTO.getDate());
        //practice.setTime(practiceDTO.getTime());
        return qualifying;
    }

    public static Sprint toSprint(SprintDTO sprintDTO) {
        Sprint sprint = new Sprint();
        //sprint.setStartDateTime(ZonedDateTime.parse(sprintDTO.getDate() + "T" + sprintDTO.getTime() + ":00.000Z"));
        return sprint;
    }
}
