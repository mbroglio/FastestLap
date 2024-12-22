package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.dto.ConstructorStandingsDTO;

public interface IConstructorStandingsRepository {
    ConstructorStandings findConstructorStandings();
}
