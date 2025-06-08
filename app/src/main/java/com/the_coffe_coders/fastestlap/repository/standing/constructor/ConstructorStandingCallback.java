package com.the_coffe_coders.fastestlap.repository.standing.constructor;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;

public interface ConstructorStandingCallback {
    void onConstructorLoaded(ConstructorStandings constructorStandings);

    void onError(Exception e);
}
