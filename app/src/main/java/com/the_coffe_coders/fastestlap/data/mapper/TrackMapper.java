package com.the_coffe_coders.fastestlap.data.mapper;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.data.dto.CircuitDTO;

public class TrackMapper {

    public static Track toTrack(CircuitDTO circuitDTO) {
        Track track = new Track();
        track.setTrackId(circuitDTO.getCircuitId());
        track.setTrackName(circuitDTO.getCircuitName());
        track.setUrl(circuitDTO.getUrl());
        track.setLocation(LocationMapper.toLocation(circuitDTO.getLocation()));
        return track;
    }
}
