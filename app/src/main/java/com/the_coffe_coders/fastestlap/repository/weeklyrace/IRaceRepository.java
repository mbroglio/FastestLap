package com.the_coffe_coders.fastestlap.repository.weeklyrace;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

import java.util.List;

public interface IRaceRepository {
    MutableLiveData<Result> fetchWeeklyRaces(long lastUpdate);
    MutableLiveData<Result> fetchNextRace(long lastUpdate);
    MutableLiveData<Result> fetchLastRace(long lastUpdate);
}

