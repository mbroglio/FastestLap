package com.the_coffe_coders.fastestlap.source.result;

import com.the_coffe_coders.fastestlap.repository.result.RaceResultCallback;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultResponseCallback;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseRaceResultRemoteDataSource {
    protected RaceResultResponseCallback raceResultCallback;

    public void setRaceResultCallback(RaceResultResponseCallback raceResultCallback) {
        this.raceResultCallback = raceResultCallback;
    }

    public abstract void getRaceResults(int round, RaceResultCallback resultCallback);

    public abstract void getAllRaceResults(int numberOfRaces, RaceResultCallback callback);

    public abstract void fetchRaceResult(int i, int i1, AtomicInteger successCount, AtomicInteger failureCount, int numberOfRaces, RaceResultCallback raceResultCallback);
}
