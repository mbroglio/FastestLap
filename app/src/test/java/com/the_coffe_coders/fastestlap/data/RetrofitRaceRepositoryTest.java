package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import junit.framework.TestCase;

import org.junit.Test;

public class RetrofitRaceRepositoryTest extends TestCase {

    public void testGetInstance() {
    }

    public void testGetLastRaceFromServer() {
        RetrofitRaceRepository raceRepository = RetrofitRaceRepository.getInstance();
        Race lastRace = raceRepository.getLastRaceFromServer().join();
        System.out.println("Last race: " + lastRace);
        assertTrue(true);

    }
}