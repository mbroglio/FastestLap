package com.the_coffe_coders.fastestlap.source.f1.standing.constructor;

import com.the_coffe_coders.fastestlap.repository.f1.standing.constructor.ConstructorStandingCallback;

public interface ConstructorStandingDataSource {
    void getConstructorStandings(ConstructorStandingCallback callback);
}