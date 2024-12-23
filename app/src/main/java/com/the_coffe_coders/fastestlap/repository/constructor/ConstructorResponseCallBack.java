package com.the_coffe_coders.fastestlap.repository.constructor;

import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

import java.util.List;

public interface ConstructorResponseCallBack {
    void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorStandingsAPIResponse, long lastUpdate);
    void onFailureFromRemote(Exception e);
    void onSuccessFromLocal(List<Constructor> constructors);
    void onFailureFromLocal(Exception e);

}
