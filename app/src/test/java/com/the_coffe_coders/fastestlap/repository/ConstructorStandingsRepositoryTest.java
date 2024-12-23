package com.the_coffe_coders.fastestlap.repository;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorStandingsRepository;

import junit.framework.TestCase;

public class ConstructorStandingsRepositoryTest extends TestCase {

    public void testFind() {
        ConstructorStandingsRepository constructorStandingsRepository = new ConstructorStandingsRepository();
        ConstructorStandings constructorStandings = constructorStandingsRepository.findConstructorStandings();
        assertNotNull(constructorStandings);
    }
}