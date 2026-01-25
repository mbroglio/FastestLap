package com.the_coffe_coders.fastestlap.repository.f1.standing.constructor;

import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;

public interface ConstructorStandingCallback {
    void onConstructorLoaded(ConstructorStandings constructorStandings);

    void onError(Exception e);
}
