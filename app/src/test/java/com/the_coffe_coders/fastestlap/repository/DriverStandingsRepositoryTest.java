package com.the_coffe_coders.fastestlap.repository;

import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.mapper.DriverStandingsMapper;

import junit.framework.TestCase;

public class DriverStandingsRepositoryTest extends TestCase {

    public void testMain() {
        DriverStandingsRepository driverStandingsRepository = new DriverStandingsRepository();
        System.out.println(DriverStandingsMapper.toDriverStandings(driverStandingsRepository.find()));
    }

    public void testFindDriverStandings() {
        DriverStandingsRepository driverStandingsRepository = new DriverStandingsRepository();
        DriverStandings driverStandings = driverStandingsRepository.findDriverStandings();
        System.out.println(driverStandings);
        assertNotNull(driverStandings);
    }
}