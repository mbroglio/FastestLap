package com.the_coffe_coders.fastestlap.mapper;

import android.util.Log;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
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
        for (ResultDTO resultDTO : raceDTO.getResults()) {
            race.addResult(SessionMapper.toResult(resultDTO));
        }
        return race;
    }

}
