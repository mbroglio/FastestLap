package com.the_coffe_coders.fastestlap.repository.result;

import com.the_coffe_coders.fastestlap.api.RaceResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;

import java.util.List;

public interface RaceResultResponseCallback {

    void onSuccessFromRemote(RaceResultsAPIResponse raceResultsAPIResponse);
    void onSuccessFromRemote(RaceResultsAPIResponse raceResultsAPIResponse, int type);
    void onFailureFromRemote(Exception exception);

    void onSuccessFromLocal(RaceResult raceResult);

    void onSuccessFromLocal(List<RaceResult> raceResultList);

    void onFailureFromLocal(Exception exception);

}
