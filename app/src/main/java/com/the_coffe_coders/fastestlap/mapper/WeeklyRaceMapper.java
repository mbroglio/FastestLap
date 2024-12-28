package com.the_coffe_coders.fastestlap.mapper;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceClassic;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceSprint;
import com.the_coffe_coders.fastestlap.dto.RaceDTO;

public class WeeklyRaceMapper {
    public static WeeklyRace toWeeklyRace(RaceDTO race) {
        WeeklyRace weeklyRace;
        if(race.getSprint() != null) {
            weeklyRace = toRaceSprint(race);
        } else {
            weeklyRace = toRaceClassic(race);
        }
        weeklyRace.setRaceName(race.getRaceName());
        //weeklyRace.setDate(race.getDate());
        weeklyRace.setRound(race.getRound());
        weeklyRace.setSeason(race.getSeason());
        weeklyRace.setCircuit(CircuitMapper.toCircuit(race.getCircuit()));
        weeklyRace.setQualifying(SessionMapper.toQualifying(race.getQualifying()));
        weeklyRace.setFirstPractice(SessionMapper.toPractice(race.getFirstPractice(), 1));
        //weeklyRace.setFinalRace(SessionMapper.toRace(race.getFinalRace()));
        return weeklyRace;
    }

    public static WeeklyRaceClassic toRaceClassic(RaceDTO race) {
        return new WeeklyRaceClassic(SessionMapper.toPractice(race.getSecondPractice(), 2), SessionMapper.toPractice(race.getThirdPractice(), 3));
    }

    public static WeeklyRaceSprint toRaceSprint(RaceDTO race) {
        return new WeeklyRaceSprint(SessionMapper.toQualifying(race.getSprintQualifying()), SessionMapper.toSprint(race.getSprint()));
    }
}
