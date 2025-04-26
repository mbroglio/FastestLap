package com.the_coffe_coders.fastestlap.source.result;

import com.the_coffe_coders.fastestlap.repository.result.RaceResultCallback;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseRaceResultRemoteDataSource {
    public abstract void getRaceResults(int round, RaceResultCallback resultCallback);

    public abstract void fetchRaceResult(int i, int i1, AtomicInteger successCount, AtomicInteger failureCount, int numberOfRaces, RaceResultCallback raceResultCallback);
}
