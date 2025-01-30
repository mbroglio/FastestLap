package com.the_coffe_coders.fastestlap.repository.circuit;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;

import java.util.Collections;
import java.util.List;

public class TrackRepository implements ITrackRepository{
    private List<Track> tracks;
    private static TrackRepository instance;


    @Override
    public List<Track> findAll() {
        return Collections.emptyList();
    }

    @Override
    public Track find(String circuitId) {
        return null;
    }
}
