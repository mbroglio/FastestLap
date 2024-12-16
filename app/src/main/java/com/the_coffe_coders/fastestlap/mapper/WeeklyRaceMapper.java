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
        return weeklyRace;
    }

    public static WeeklyRaceClassic toRaceClassic(RaceDTO race) {
        WeeklyRaceClassic weeklyRace = new WeeklyRaceClassic();
        weeklyRace.setRaceName(race.getRaceName());
        weeklyRace.setDate(race.getDate());
        weeklyRace.setRound(race.getRound());
        weeklyRace.setSeason(race.getSeason());
        weeklyRace.setCircuit(CircuitMapper.toCircuit(race.getCircuit()));
        return weeklyRace;
    }

    public static WeeklyRaceSprint toRaceSprint(RaceDTO race) {
        WeeklyRaceSprint weeklyRace = new WeeklyRaceSprint();
        return weeklyRace;
    }
}
