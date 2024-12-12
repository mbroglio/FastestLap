package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import junit.framework.TestCase;

public class RetrofitRaceRepositoryTest extends TestCase {

    public void testGetInstance() {
    }

    public void testGetLastRaceFromServer() {
        JolpicaRaceRepository raceRepository = JolpicaRaceRepository.getInstance();
        Race lastRace = raceRepository.getLastRaceFromServer().join();
        System.out.println("Last race: " + lastRace);
        assertTrue(true);

    }
}