package com.the_coffe_coders.fastestlap.repository.standings;

import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;

public interface ConstructorStandingsResponseCallback {
    void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorStandingsAPIResponse, long lastUpdate);

    void onFailureFromRemote(Exception e);

    void onSuccessFromLocal(ConstructorStandings constructorStandings);

    void onFailureFromLocal(Exception e);

}
