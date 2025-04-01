package com.the_coffe_coders.fastestlap.repository.weeklyrace;

import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

public interface SingleWeeklyRaceCallback {
    public void onSuccess(WeeklyRace weeklyRace);
    public void onFailure(Exception exception);
}
