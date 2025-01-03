package com.the_coffe_coders.fastestlap.mapper;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceClassic;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceSprint;
import com.the_coffe_coders.fastestlap.dto.RaceDTO;

public class WeeklyRaceMapper {
    public static WeeklyRace toWeeklyRace(RaceDTO raceDTO) {
        WeeklyRace weeklyRace;
        if(raceDTO.getSprint() != null) {
            weeklyRace = toRaceSprint(raceDTO);
        } else {
            weeklyRace = toRaceClassic(raceDTO);
        }
        weeklyRace.setRaceName(raceDTO.getRaceName());
        weeklyRace.setRound(raceDTO.getRound());
        weeklyRace.setSeason(raceDTO.getSeason());
        weeklyRace.setCircuit(CircuitMapper.toCircuit(raceDTO.getCircuit()));
        weeklyRace.setQualifying(SessionMapper.toQualifying(raceDTO.getQualifying()));
        weeklyRace.setFirstPractice(SessionMapper.toPractice(raceDTO.getFirstPractice(), 1));
        weeklyRace.setUrl(raceDTO.getUrl());

        Race finalRace = new Race();
        finalRace.setSeason(raceDTO.getSeason());
        finalRace.setRound(raceDTO.getRound());
        finalRace.setRaceName(raceDTO.getRaceName());
        finalRace.setUrl(raceDTO.getUrl());
        finalRace.setCircuit(CircuitMapper.toCircuit(raceDTO.getCircuit()));
        finalRace.setStartDateTime(raceDTO.getDate(), raceDTO.getTime());
        finalRace.setEndDateTime();
        /*
        List<RaceResult> results = new ArrayList<>();
        for (ResultDTO resultDTO: raceDTO.getResults()) {
            results.add(SessionMapper.toResult(resultDTO));
        }
        finalRace.setResults(results);*/
        weeklyRace.setFinalRace(finalRace);
        return weeklyRace;
    }

    public static WeeklyRaceClassic toRaceClassic(RaceDTO race) {
        return new WeeklyRaceClassic(SessionMapper.toPractice(race.getSecondPractice(), 2),
                SessionMapper.toPractice(race.getThirdPractice(), 3));
    }

    public static WeeklyRaceSprint toRaceSprint(RaceDTO race) {
        return new WeeklyRaceSprint(SessionMapper.toQualifying(race.getSprintQualifying()),
                SessionMapper.toSprint(race.getSprint()));
    }
}
