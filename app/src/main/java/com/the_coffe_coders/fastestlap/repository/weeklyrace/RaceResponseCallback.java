package com.the_coffe_coders.fastestlap.repository.weeklyrace;

import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

import java.util.List;

public interface RaceResponseCallback {
    void onSuccessFromRemote(RaceAPIResponse weeklyRaceAPIResponse, OperationType operationType);
    void onFailureFromRemote(Exception exception);
    void onSuccessFromLocal(List<WeeklyRace> weeklyRaceList);
    void onFailureFromLocal(Exception exception);
}