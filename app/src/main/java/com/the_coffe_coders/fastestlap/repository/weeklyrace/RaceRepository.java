package com.the_coffe_coders.fastestlap.repository.weeklyrace;


import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.dto.RaceDTO;
import com.the_coffe_coders.fastestlap.mapper.WeeklyRaceMapper;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceLocalDataSource;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceRemoteDataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RaceRepository implements IRaceRepository, RaceResponseCallback {
    private final MutableLiveData<Result> weeklyRaceMutableLiveData;
    private final MutableLiveData<Result> nextRaceMutableLiveData;
    private final MutableLiveData<Result> lastRaceMutableLiveData;
    private final BaseWeeklyRaceRemoteDataSource raceRemoteDataSource;
    private final BaseWeeklyRaceLocalDataSource raceLocalDataSource;

    private static final String TAG = "RaceRepository";
    public RaceRepository(BaseWeeklyRaceRemoteDataSource raceRemoteDataSource, BaseWeeklyRaceLocalDataSource raceLocalDataSource) {
        this.weeklyRaceMutableLiveData = new MutableLiveData<>();
        this.nextRaceMutableLiveData = new MutableLiveData<>();
        this.lastRaceMutableLiveData = new MutableLiveData<>();
        this.raceRemoteDataSource = raceRemoteDataSource;
        this.raceLocalDataSource = raceLocalDataSource;
        this.raceRemoteDataSource.setRaceCallback(this);
        this.raceLocalDataSource.setRaceCallback(this);
    }

    @Override
    public MutableLiveData<Result> fetchWeeklyRaces(long lastUpdate) {
        Log.i(TAG, "fetchWeeklyRaces");
        if(true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT)
            //TODO fetch from remote
            raceRemoteDataSource.getWeeklyRaces();
        }else {
            raceLocalDataSource.getWeeklyRaces();
        }
        return weeklyRaceMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> fetchNextRace(long lastUpdate) {
        return null;
    }

    @Override
    public MutableLiveData<Result> fetchLastRace(long lastUpdate) {
        return null;
    }

    @Override
    public void onSuccessFromRemote(RaceAPIResponse weeklyRaceAPIResponse, long lastUpdate) {
        Log.i(TAG, "onSuccessFromRemote");
        List<RaceDTO> raceDTOS = weeklyRaceAPIResponse.getRaceTable().getRaces();
        List<WeeklyRace> weeklyRaceList = new ArrayList<>();
        for (RaceDTO raceDTO: raceDTOS) {
            weeklyRaceList.add(WeeklyRaceMapper.toWeeklyRace(raceDTO));
        }

        //Collections.reverse(weeklyRaceList);
        raceLocalDataSource.insertWeeklyRaceList(weeklyRaceList);
    }

    @Override
    public void onFailureFromRemote(Exception exception) {

    }

    @Override
    public void onSuccessFromLocal(List<WeeklyRace> weeklyRaceList) {
        Log.i(TAG, "onSuccessFromLocal");
        Result.WeeklyRaceSuccess result = new Result.WeeklyRaceSuccess(weeklyRaceList);
        for (WeeklyRace weeklyRace: weeklyRaceList) {
            Log.i(TAG, "WEEKLY RACES:\n " + weeklyRace.getRaceName());
        }
        weeklyRaceMutableLiveData.postValue(result);
    }

    @Override
    public void onFailureFromLocal(Exception exception) {

    }
}
