package com.the_coffe_coders.fastestlap.repository.constructor;

import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;

public interface ConstructorResponseCallback {
    void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorStandingsAPIResponse, long lastUpdate);

    void onFailureFromRemote(Exception e);

    void onSuccessFromLocal(ConstructorStandings constructorStandings);

    void onFailureFromLocal(Exception e);

}
