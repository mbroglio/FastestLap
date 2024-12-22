package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.mapper.DriverStandingsMapper;

import junit.framework.TestCase;

public class JolpicaDriverStandingsRepositoryTest extends TestCase {

    public void testMain() {
        JolpicaDriverStandingsRepository jolpicaDriverStandingsRepository = new JolpicaDriverStandingsRepository();
        System.out.println(DriverStandingsMapper.toDriverStandings(jolpicaDriverStandingsRepository.find()));
    }

    public void testFindDriverStandings() {
        JolpicaDriverStandingsRepository jolpicaDriverStandingsRepository = new JolpicaDriverStandingsRepository();
        DriverStandings driverStandings = jolpicaDriverStandingsRepository.findDriverStandings();
        System.out.println(driverStandings);
        assertNotNull(driverStandings);
    }
}