package com.the_coffe_coders.fastestlap.repository.weeklyrace;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

public interface SingleWeeklyRaceCallback {
    void onSuccess(WeeklyRace weeklyRace);

    void onFailure(Exception exception);
}
