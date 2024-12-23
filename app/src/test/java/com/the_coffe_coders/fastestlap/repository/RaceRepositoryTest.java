package com.the_coffe_coders.fastestlap.repository;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

import junit.framework.TestCase;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class RaceRepositoryTest extends TestCase {

    public void testGetInstance() {
    }

    public void testRacesFromServer() throws ExecutionException, InterruptedException {
        RaceRepository raceRepository = RaceRepository.getInstance();
        List<WeeklyRace> races = raceRepository.getRacesFromServer();
        int cont = 0;
        System.out.println("Races: ");
        for (WeeklyRace race: races) {
            System.out.println(cont + ")" + race);
            cont++;
        }
        assertTrue(true);

    }

    public void testNextRaceFromServer() {
        RaceRepository raceRepository = RaceRepository.getInstance();
        try {
            System.out.println(raceRepository.getNextRaceFromServer());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        //System.out.println("Last race: " + lastRace);
        assertTrue(true);
    }

    public void testLastRaceResultFromServer() {
        RaceRepository raceRepository = RaceRepository.getInstance();
        WeeklyRace wr = raceRepository.getLastRaceResultFromServer();
        System.out.println("Last race: " + wr);
        assertTrue(true);
    }
}