package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.dto.ConstructorStandingsDTO;
import com.the_coffe_coders.fastestlap.mapper.ConstructorStandingsMapper;

import junit.framework.TestCase;

public class JolpicaConstructorStandingsRepositoryTest extends TestCase {

    public void testFind() {
        JolpicaConstructorStandingsRepository jolpicaConstructorStandingsRepository = new JolpicaConstructorStandingsRepository();
        ConstructorStandings constructorStandingsDTO = jolpicaConstructorStandingsRepository.findConstructorStandings();
    }
}