package com.the_coffe_coders.fastestlap.repository.weeklyrace;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

import java.util.List;

public interface WeeklyRacesCallback {
    void onSuccess(List<WeeklyRace> weeklyRaces);

    void onFailure(Exception exception);
}
