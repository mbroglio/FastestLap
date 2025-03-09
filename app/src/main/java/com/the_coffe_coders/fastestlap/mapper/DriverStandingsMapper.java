package com.the_coffe_coders.fastestlap.mapper;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.dto.ConstructorDTO;
import com.the_coffe_coders.fastestlap.dto.DriverStandingsDTO;
import com.the_coffe_coders.fastestlap.dto.DriverStandingsElementDTO;

import java.util.ArrayList;
import java.util.List;

public class DriverStandingsMapper {
    public static DriverStandings toDriverStandings(DriverStandingsDTO driverStandingsDTO) {
        DriverStandings driverStandings = new DriverStandings();

        driverStandings.setSeason(driverStandingsDTO.getSeason());
        driverStandings.setRound(driverStandingsDTO.getRound());
        List<DriverStandingsElement> driverStandingsElements = new ArrayList<>();

        for (DriverStandingsElementDTO dseDTO : driverStandingsDTO.getDriverStandings()) {
            driverStandingsElements.add(toDriverStandingsElement(dseDTO));
        }
        driverStandings.setDriverStandingsElements(driverStandingsElements);

        return driverStandings;
    }

    public static DriverStandingsElement toDriverStandingsElement(DriverStandingsElementDTO driverStandingsElementDTO) {
        DriverStandingsElement driverStandingsElement = new DriverStandingsElement();

        driverStandingsElement.setDriver(DriverMapper.toDriver(driverStandingsElementDTO.getDriver()));
        List<Constructor> constructors = new ArrayList<>();
        for (ConstructorDTO cDTO : driverStandingsElementDTO.getConstructors()) {
            constructors.add(ConstructorMapper.toConstructor(cDTO));
        }
        driverStandingsElement.setConstructors(constructors);
        driverStandingsElement.setPoints(driverStandingsElementDTO.getPoints());
        driverStandingsElement.setWins(driverStandingsElementDTO.getWins());
        driverStandingsElement.setPositionText(driverStandingsElementDTO.getPositionText());
        driverStandingsElement.setPosition(driverStandingsElementDTO.getPosition());
        return driverStandingsElement;
    }
}
