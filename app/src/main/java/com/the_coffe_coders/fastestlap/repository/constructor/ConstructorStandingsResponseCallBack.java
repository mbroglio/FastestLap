package com.the_coffe_coders.fastestlap.repository.constructor;

import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;

import java.util.List;

public interface ConstructorStandingsResponseCallBack {
    void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorStandingsAPIResponse, long lastUpdate);
    void onFailureFromRemote(Exception e);
    void onSuccessFromLocal(List<ConstructorStandings> constructorsStandings);
    void onFailureFromLocal(Exception e);
}
