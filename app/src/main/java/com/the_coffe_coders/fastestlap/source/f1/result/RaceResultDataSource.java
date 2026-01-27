package com.the_coffe_coders.fastestlap.source.f1.result;

import com.the_coffe_coders.fastestlap.repository.f1.result.RaceResultCallback;

public interface RaceResultDataSource {
    void getRaceResults(int id, RaceResultCallback raceResultCallback);
}
