package com.the_coffe_coders.fastestlap.source.standing.constructor;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class BaseConstructorStandingsLocalDataSource {
    public abstract void insertConstructorsStandings(ConstructorStandings constructorStandings);

    public abstract void getConstructorStandings();
}