package com.the_coffe_coders.fastestlap.mapper;

import android.util.Log;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceClassic;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRaceSprint;
import com.the_coffe_coders.fastestlap.dto.RaceDTO;
import com.the_coffe_coders.fastestlap.dto.ResultDTO;

import java.util.ArrayList;
import java.util.List;

public class WeeklyRaceMapper {
    public static WeeklyRace toWeeklyRace(RaceDTO race) {
        WeeklyRace weeklyRace;
        if(race.getSprint() != null) {
            weeklyRace = toRaceSprint(race);
        } else {
            weeklyRace = toRaceClassic(race);
        }
        weeklyRace.setRaceName(race.getRaceName());
        weeklyRace.setRound(race.getRound());
        weeklyRace.setSeason(race.getSeason());
        weeklyRace.setCircuit(CircuitMapper.toCircuit(race.getCircuit()));
        weeklyRace.setQualifying(SessionMapper.toQualifying(race.getQualifying()));
        weeklyRace.setFirstPractice(SessionMapper.toPractice(race.getFirstPractice(), 1));
        weeklyRace.setUrl(race.getUrl());
        weeklyRace.setDateTime(race.getDate(), race.getTime());

        Race finalRace = new Race();
        finalRace.setSeason(race.getSeason());
        finalRace.setRound(race.getRound());
        finalRace.setRaceName(race.getRaceName());
        finalRace.setUrl(race.getUrl());
        finalRace.setCircuit(CircuitMapper.toCircuit(race.getCircuit()));
        finalRace.setDateTime(weeklyRace.getDateTime());
        /*
        List<RaceResult> results = new ArrayList<>();
        for (ResultDTO resultDTO: race.getResults()) {
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
