package com.the_coffe_coders.fastestlap.source.constructor;

import com.the_coffe_coders.fastestlap.repository.standings.ConstructorStandingsResponseCallback;

import lombok.Setter;

@Setter
public abstract class BaseConstructorStandingsRemoteDataSource {
    protected ConstructorStandingsResponseCallback constructorCallback;

    public abstract void getConstructorStandings();
}
