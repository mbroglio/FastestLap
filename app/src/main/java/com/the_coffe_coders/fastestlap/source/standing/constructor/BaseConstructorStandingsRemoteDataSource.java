package com.the_coffe_coders.fastestlap.source.standing.constructor;

import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorCallback;
import com.the_coffe_coders.fastestlap.repository.standing.constructor.ConstructorStandingCallback;

import lombok.Setter;

@Setter
public abstract class BaseConstructorStandingsRemoteDataSource {
    public abstract void getConstructorStandings(ConstructorStandingCallback constructorCallback);
}
