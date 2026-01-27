package com.the_coffe_coders.fastestlap.repository.f1.result;

import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;

public interface RaceResultCallback {
    void onSuccess(Race race);

    void onFailure(Exception exception);
}
