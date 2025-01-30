package com.the_coffe_coders.fastestlap.repository;

import junit.framework.TestCase;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.repository.circuit.TrackRepository;

import java.util.List;

public class JolpicaTrackRepositoryTest extends TestCase {

    public void testFindAll() {
        TrackRepository trackRepository = new TrackRepository();
        List<Track> tracks = trackRepository.findAll();

        System.out.println("Circuits:");

        if(tracks.isEmpty()){
            System.out.println("Nessun circuito trovato");
        }

        for (Track track : tracks) {
            System.out.println(track.getTrackName());
        }
        assertTrue(true);
    }

    public void testFind() {

    }
}