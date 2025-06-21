package com.the_coffe_coders.fastestlap.mapper;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.dto.QualifyingResultDTO;
import com.the_coffe_coders.fastestlap.dto.RaceDTO;
import com.the_coffe_coders.fastestlap.dto.ResultDTO;

public class RaceMapper {
    public static Race toRace(RaceDTO raceDTO) {
        Race race = new Race();
        race.setSeason(raceDTO.getSeason());
        race.setRound(raceDTO.getRound());
        race.setRaceName(raceDTO.getRaceName());
        race.setUrl(raceDTO.getUrl());
        race.setStartDateTime(raceDTO.getDate(), raceDTO.getTime());
        race.setEndDateTime();
        race.setTrack(TrackMapper.toTrack(raceDTO.getCircuit()));
        if (raceDTO.getResults() != null && !raceDTO.getResults().isEmpty()) {
            for (ResultDTO resultDTO : raceDTO.getResults()) {
                race.addResult(SessionMapper.toResult(resultDTO));
            }
        }else{
            raceDTO.setResults(null);
        }

        if(raceDTO.getQualifyingResults() != null && !raceDTO.getQualifyingResults().isEmpty()) {
            for (QualifyingResultDTO qualifyingResultDTO : raceDTO.getQualifyingResults()) {
                race.addQualifyingResult(SessionMapper.toQualifyingResult(qualifyingResultDTO));
            }
        }else{
            raceDTO.setQualifyingResults(null);
        }

        if (raceDTO.getSprintResults() != null && !raceDTO.getSprintResults().isEmpty()) {
            for (ResultDTO sprintResultDTO : raceDTO.getSprintResults()) {
                race.addSprintResult(SessionMapper.toResult(sprintResultDTO));
            }
        }else{
            raceDTO.setSprintResults(null);
        }

        return race;
    }

}
