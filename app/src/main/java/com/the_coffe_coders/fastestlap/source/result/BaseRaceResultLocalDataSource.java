package com.the_coffe_coders.fastestlap.source.result;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultResponseCallback;

import java.util.List;

public abstract class BaseRaceResultLocalDataSource {
    protected RaceResultResponseCallback raceResultCallback;

    public void setRaceResultCallback(RaceResultResponseCallback raceResultCallback) {
        this.raceResultCallback = raceResultCallback;
    }

    public abstract void getAllRaceResult();

    public abstract void getAllRaceList();

    public abstract void getRaceResultById(int id);

    public abstract void insertRaceList(List<Race> raceList);

    public abstract void insertRaceResultList(List<RaceResult> raceResultList);
}
