package com.the_coffe_coders.fastestlap.repository.weeklyrace;


import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceLocalDataSource;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceRemoteDataSource;

import java.util.Collections;
import java.util.List;

public class RaceRepository implements IRaceRepository, RaceResponseCallback {
    private final MutableLiveData<Result> weeklyRaceMutableLiveData;
    private final MutableLiveData<Result> nextRaceMutableLiveData;
    private final MutableLiveData<Result> lastRaceMutableLiveData;
    private final BaseWeeklyRaceRemoteDataSource raceRemoteDataSource;
    private final BaseWeeklyRaceLocalDataSource raceLocalDataSource;

    private static final String TAG = "RaceRepository";
    private RaceRepository(BaseWeeklyRaceRemoteDataSource raceRemoteDataSource, BaseWeeklyRaceLocalDataSource raceLocalDataSource) {
        this.weeklyRaceMutableLiveData = new MutableLiveData<>();
        this.nextRaceMutableLiveData = new MutableLiveData<>();
        this.lastRaceMutableLiveData = new MutableLiveData<>();
        this.raceRemoteDataSource = raceRemoteDataSource;
        this.raceLocalDataSource = raceLocalDataSource;
    }

    @Override
    public List<WeeklyRace> fetchWeeklyRace(long lastUpdate) {
        if(true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT)
            //TODO fetch from remote raceRemoteDataSource.getWeeklyRace();
        }else {
            raceLocalDataSource.getWeeklyRace();
        }
        return Collections.emptyList();
    }

    @Override
    public WeeklyRace fetchNextRace(long lastUpdate) {
        return null;
    }

    @Override
    public WeeklyRace fetchLastRace(long lastUpdate) {
        return null;
    }

    @Override
    public void onSuccessFromRemote(RaceAPIResponse weeklyRaceAPIResponse, long lastUpdate) {

    }

    @Override
    public void onFailureFromRemote(Exception exception) {

    }

    @Override
    public void onSuccessFromLocal(List<WeeklyRace> weeklyRaceList) {
        Log.i(TAG, "onSuccessFromLocal");
        Result.WeeklyRaceSuccess result = new Result.WeeklyRaceSuccess(weeklyRaceList);
        weeklyRaceMutableLiveData.postValue(result);
    }

    @Override
    public void onFailureFromLocal(Exception exception) {

    }
}
