package com.the_coffe_coders.fastestlap.source.standing.constructor;

import com.the_coffe_coders.fastestlap.repository.standing.constructor.ConstructorStandingCallback;

public interface ConstructorStandingDataSource {
    void getConstructorStandings(ConstructorStandingCallback callback);
}