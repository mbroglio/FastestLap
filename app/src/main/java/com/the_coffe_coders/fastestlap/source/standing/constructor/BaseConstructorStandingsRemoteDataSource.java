package com.the_coffe_coders.fastestlap.source.standing.constructor;

import com.the_coffe_coders.fastestlap.repository.standing.constructor.ConstructorStandingsResponseCallback;

import lombok.Setter;

@Setter
public abstract class BaseConstructorStandingsRemoteDataSource {
    protected ConstructorStandingsResponseCallback constructorCallback;

    public abstract void getConstructorStandings();
}
