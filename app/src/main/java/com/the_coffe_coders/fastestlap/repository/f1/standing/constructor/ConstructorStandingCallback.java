package com.the_coffe_coders.fastestlap.repository.f1.standing.constructor;

import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;

import java.util.List;

public interface ConstructorStandingCallback {
    void onConstructorLoaded(ConstructorStandings constructorStandings);

    void onError(Exception e);

    void onConstructorListLoaded(List<Constructor> constructorList);
}
