package com.the_coffe_coders.fastestlap.mapper;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.dto.ConstructorStandingsElementDTO;
import com.the_coffe_coders.fastestlap.dto.ConstructorStandingsDTO;

import java.util.ArrayList;
import java.util.List;

public class ConstructorStandingsMapper {
    public static ConstructorStandings toConstructorStandings(ConstructorStandingsDTO constructorStandingsDTO) {
        ConstructorStandings constructorStandings = new ConstructorStandings();
        constructorStandings.setSeason(constructorStandingsDTO.getSeason());
        constructorStandings.setRound(constructorStandingsDTO.getRound());
        List<ConstructorStandingsElement> constructorStandingsElements = new ArrayList<>();
        for (ConstructorStandingsElementDTO constructorStandingElementDTO : constructorStandingsDTO.getConstructorStandings()) {
            constructorStandingsElements.add(toConstructorStandingsElement(constructorStandingElementDTO));
        }
        constructorStandings.setConstructorStandings(constructorStandingsElements);
        return constructorStandings;
    }

    private static ConstructorStandingsElement toConstructorStandingsElement(ConstructorStandingsElementDTO constructorStandingsElementDTO) {
        ConstructorStandingsElement constructorStandingsElement = new ConstructorStandingsElement();
        constructorStandingsElement.setPosition(constructorStandingsElementDTO.getPosition());
        constructorStandingsElement.setPositionText(constructorStandingsElementDTO.getPositionText());
        constructorStandingsElement.setPoints(constructorStandingsElementDTO.getPoints());
        constructorStandingsElement.setConstructor(ConstructorMapper.toConstructor(constructorStandingsElementDTO.getConstructor()));
        constructorStandingsElement.setWins(constructorStandingsElementDTO.getWins());
        return constructorStandingsElement;
    }
}
