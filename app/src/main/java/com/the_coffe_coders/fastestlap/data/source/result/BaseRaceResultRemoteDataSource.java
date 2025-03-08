package com.the_coffe_coders.fastestlap.data.source.result;

import com.the_coffe_coders.fastestlap.data.repository.result.RaceResultResponseCallback;

public abstract class BaseRaceResultRemoteDataSource {
    protected RaceResultResponseCallback raceResultCallback;

    public void setRaceResultCallback(RaceResultResponseCallback raceResultCallback) {
        this.raceResultCallback = raceResultCallback;
    }

    public abstract void getRaceResults(int round);

    public abstract void getAllRaceResults();

    public abstract void getLastRaceResults();
}
