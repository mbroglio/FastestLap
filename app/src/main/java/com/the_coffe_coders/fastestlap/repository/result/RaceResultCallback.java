package com.the_coffe_coders.fastestlap.repository.result;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;

public interface RaceResultCallback {
    void onSuccess(Race race);

    void onFailure(Exception exception);
}
