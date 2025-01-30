package com.the_coffe_coders.fastestlap.repository.circuit;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;

import java.util.List;

public interface ITrackRepository {
    List<Track> findAll();

    Track find(String circuitId);
}
