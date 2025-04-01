package com.the_coffe_coders.fastestlap.source.standing.constructor;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;

import lombok.Setter;

@Setter
public abstract class BaseConstructorStandingsLocalDataSource {
    public abstract void insertConstructorsStandings(ConstructorStandings constructorStandings);

    public abstract void getConstructorStandings();
}