package com.the_coffe_coders.fastestlap.mapper;

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
    public static WeeklyRace toWeeklyRace(RaceDTO raceDTO) {
        WeeklyRace weeklyRace;
        if (raceDTO.getSprint() != null) {
            weeklyRace = toRaceSprint(raceDTO);
        } else {
            weeklyRace = toRaceClassic(raceDTO);
        }
        weeklyRace.setRaceName(raceDTO.getRaceName());
        weeklyRace.setRound(raceDTO.getRound());
        weeklyRace.setSeason(raceDTO.getSeason());
        weeklyRace.setTrack(TrackMapper.toTrack(raceDTO.getCircuit()));
        weeklyRace.setQualifying(SessionMapper.toQualifying(raceDTO.getQualifying()));
        weeklyRace.setFirstPractice(SessionMapper.toPractice(raceDTO.getFirstPractice(), 1));
        weeklyRace.setUrl(raceDTO.getUrl());

        Race finalRace = new Race();
        finalRace.setSeason(raceDTO.getSeason());
        finalRace.setRound(raceDTO.getRound());
        finalRace.setRaceName(raceDTO.getRaceName());
        finalRace.setUrl(raceDTO.getUrl());
        finalRace.setTrack(TrackMapper.toTrack(raceDTO.getCircuit()));
        finalRace.setStartDateTime(raceDTO.getDate(), raceDTO.getTime());
        finalRace.setEndDateTime();

        /*List<RaceResult> results = new ArrayList<>();
        for (ResultDTO resultDTO: raceDTO.getResults()) {
            results.add(SessionMapper.toResult(resultDTO));
        }
        finalRace.setResults(results);
        */


        weeklyRace.setFinalRace(finalRace);
        return weeklyRace;
    }

    public static WeeklyRace toWeeklyRaceLast(RaceDTO raceDTO) {
        WeeklyRace weeklyRace;
        if (raceDTO.getSprint() != null) {
            weeklyRace = toRaceSprint(raceDTO);
        } else {
            weeklyRace = toRaceClassic(raceDTO);
        }
        weeklyRace.setRaceName(raceDTO.getRaceName());
        weeklyRace.setRound(raceDTO.getRound());
        weeklyRace.setSeason(raceDTO.getSeason());
        weeklyRace.setTrack(TrackMapper.toTrack(raceDTO.getCircuit()));
        weeklyRace.setQualifying(SessionMapper.toQualifying(raceDTO.getQualifying()));
        weeklyRace.setFirstPractice(SessionMapper.toPractice(raceDTO.getFirstPractice(), 1));
        weeklyRace.setUrl(raceDTO.getUrl());

        Race finalRace = new Race();
        finalRace.setSeason(raceDTO.getSeason());
        finalRace.setRound(raceDTO.getRound());
        finalRace.setRaceName(raceDTO.getRaceName());
        finalRace.setUrl(raceDTO.getUrl());
        finalRace.setTrack(TrackMapper.toTrack(raceDTO.getCircuit()));
        finalRace.setStartDateTime(raceDTO.getDate(), raceDTO.getTime());
        finalRace.setEndDateTime();

        List<RaceResult> results = new ArrayList<>();
        for (ResultDTO resultDTO : raceDTO.getResults()) {
            results.add(SessionMapper.toResult(resultDTO));
        }
        finalRace.setResults(results);


        weeklyRace.setFinalRace(finalRace);
        return weeklyRace;
    }

    public static WeeklyRaceClassic toRaceClassic(RaceDTO race) {
        WeeklyRaceClassic weeklyRaceClassic = new WeeklyRaceClassic();
        weeklyRaceClassic.setSecondPractice(SessionMapper.toPractice(race.getSecondPractice(), 2));
        weeklyRaceClassic.setThirdPractice(SessionMapper.toPractice(race.getThirdPractice(), 3));
        return weeklyRaceClassic;
    }

    public static WeeklyRaceSprint toRaceSprint(RaceDTO race) {
        return new WeeklyRaceSprint(SessionMapper.toSprintQualifying(race.getSprintQualifying()),
                SessionMapper.toSprint(race.getSprint()));
    }

    public static WeeklyRace toWeeklyRaceNext(RaceDTO raceDTO) { // ???
        WeeklyRace weeklyRace;
        if (raceDTO.getSprint() != null) {
            weeklyRace = toRaceSprint(raceDTO);
        } else {
            weeklyRace = toRaceClassic(raceDTO);
        }
        weeklyRace.setRaceName(raceDTO.getRaceName());
        weeklyRace.setRound(raceDTO.getRound());
        weeklyRace.setSeason(raceDTO.getSeason());
        weeklyRace.setTrack(TrackMapper.toTrack(raceDTO.getCircuit()));
        if (raceDTO.getQualifying() != null) {
            weeklyRace.setQualifying(SessionMapper.toQualifying(raceDTO.getQualifying()));
        }
        weeklyRace.setFirstPractice(SessionMapper.toPractice(raceDTO.getFirstPractice(), 1));
        weeklyRace.setUrl(raceDTO.getUrl());

        Race finalRace = new Race();
        finalRace.setSeason(raceDTO.getSeason());
        finalRace.setRound(raceDTO.getRound());
        finalRace.setRaceName(raceDTO.getRaceName());
        finalRace.setUrl(raceDTO.getUrl());
        finalRace.setTrack(TrackMapper.toTrack(raceDTO.getCircuit()));
        finalRace.setStartDateTime(raceDTO.getDate(), raceDTO.getTime());
        finalRace.setEndDateTime();

        /*List<RaceResult> results = new ArrayList<>();
        for (ResultDTO resultDTO: raceDTO.getResults()) {
            results.add(SessionMapper.toResult(resultDTO));
        }
        finalRace.setResults(results);
        */


        weeklyRace.setFinalRace(finalRace);
        return weeklyRace;
    }
}
