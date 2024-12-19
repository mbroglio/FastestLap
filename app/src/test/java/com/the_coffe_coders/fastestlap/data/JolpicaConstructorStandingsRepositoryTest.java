package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.dto.ConstructorStandingsDTO;
import com.the_coffe_coders.fastestlap.mapper.ConstructorStandingsMapper;

import junit.framework.TestCase;

public class JolpicaConstructorStandingsRepositoryTest extends TestCase {

    public void testFind() {
        JolpicaConstructorStandingRepository jolpicaConstructorStandingRepository = new JolpicaConstructorStandingRepository();
        ConstructorStandingsDTO constructorStandingsDTO = jolpicaConstructorStandingRepository.find();
        System.out.println(ConstructorStandingsMapper.toConstructorStandings(constructorStandingsDTO));
    }
}