package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import junit.framework.TestCase;

import java.util.List;

public class JolpicaRaceRepositoryTest extends TestCase {

    public void testGetInstance() {
    }

    public void testRacesFromServer() {
        JolpicaRaceRepository raceRepository = JolpicaRaceRepository.getInstance();
        raceRepository.getRacesFromServer();
        //System.out.println("Last race: " + lastRace);
        assertTrue(true);

    }
}