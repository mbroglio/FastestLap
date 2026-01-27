package com.the_coffe_coders.fastestlap.repository.f1.weeklyrace;

import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;

public interface SingleWeeklyRaceCallback {
    void onSuccess(WeeklyRace weeklyRace);

    void onFailure(Exception exception);
}
