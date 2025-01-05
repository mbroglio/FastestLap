package com.the_coffe_coders.fastestlap.repository.result;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.RaceResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceLocalDataSource;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceRemoteDataSource;

public interface RaceResultResponseCallback {

    void onSuccessFromRemote(RaceResultsAPIResponse raceResultsAPIResponse);
    void onFailureFromRemote(Exception exception);
    //void onSuccessFromLocal(List<RaceResult> raceResultList);
    void onFailureFromLocal(Exception exception);

}
