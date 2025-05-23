package com.the_coffe_coders.fastestlap.source.result;

import com.the_coffe_coders.fastestlap.repository.result.RaceResultCallback;

public interface RaceResultDataSource {
    void getRaceResults(int id, RaceResultCallback raceResultCallback);
}
