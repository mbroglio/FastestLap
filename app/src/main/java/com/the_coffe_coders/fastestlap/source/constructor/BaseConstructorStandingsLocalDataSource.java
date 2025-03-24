package com.the_coffe_coders.fastestlap.source.constructor;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.repository.standings.ConstructorStandingsResponseCallback;

import lombok.Setter;

@Setter
public abstract class BaseConstructorStandingsLocalDataSource {
    protected ConstructorStandingsResponseCallback constructorCallback;

    public abstract void insertConstructorsStandings(ConstructorStandings constructorStandings);

    public abstract void getConstructorStandings();
}