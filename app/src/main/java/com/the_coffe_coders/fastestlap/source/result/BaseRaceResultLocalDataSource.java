package com.the_coffe_coders.fastestlap.source.result;

import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultResponseCallback;

import java.util.List;

public abstract class BaseRaceResultLocalDataSource {
    protected RaceResultResponseCallback raceResultCallback;
    public void setRaceResultCallback(RaceResultResponseCallback raceResultCallback) {
        this.raceResultCallback = raceResultCallback;
    }
    public abstract void getRaceResults();

    public abstract void getRaceResultById(int id);
    public abstract void insertRaceResultList(List<RaceResult> raceResultList);
}
