package com.the_coffe_coders.fastestlap.repository.weeklyrace;


import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.dto.RaceDTO;
import com.the_coffe_coders.fastestlap.mapper.WeeklyRaceMapper;
import com.the_coffe_coders.fastestlap.repository.result.IRaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceLocalDataSource;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceRemoteDataSource;

import java.util.ArrayList;
import java.util.List;

public class RaceRepository implements IRaceRepository, RaceResponseCallback {
    private static final String TAG = "RaceRepository";
    public static boolean isOutdateRace = true;
    private final MutableLiveData<Result> weeklyRaceMutableLiveData;
    private final BaseWeeklyRaceRemoteDataSource raceRemoteDataSource;
    private final MutableLiveData<Result> nextRaceMutableLiveData;
    private final MutableLiveData<Result> lastRaceMutableLiveData;
    private final BaseWeeklyRaceLocalDataSource raceLocalDataSource;

    public RaceRepository(BaseWeeklyRaceRemoteDataSource raceRemoteDataSource, BaseWeeklyRaceLocalDataSource raceLocalDataSource, RaceResultRepository raceResultRepository) {
        this.weeklyRaceMutableLiveData = new MutableLiveData<>();
        this.lastRaceMutableLiveData = new MutableLiveData<>();
        this.raceRemoteDataSource = raceRemoteDataSource;
        this.raceLocalDataSource = raceLocalDataSource;
        this.raceRemoteDataSource.setRaceCallback(this);
        this.raceLocalDataSource.setRaceCallback(this);

        this.nextRaceMutableLiveData = new MutableLiveData<>();
    }

    @Override
    public MutableLiveData<Result> fetchWeeklyRaces(long lastUpdate) {

        if (true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT)
            //TODO fetch from remote
            raceRemoteDataSource.getWeeklyRaces();
            //isOutdateRace = false;
        } else {
            Log.i(TAG, "fetchWeeklyRaces from local");
            raceLocalDataSource.getWeeklyRaces();
        }
        return weeklyRaceMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> fetchNextRace(long lastUpdate) {
        raceRemoteDataSource.getNextRace();
        return nextRaceMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> fetchLastRace(long lastUpdate) {
        raceRemoteDataSource.getLastRace();
        return lastRaceMutableLiveData;
    }

    @Override
    public void onSuccessFromRemote(RaceAPIResponse weeklyRaceAPIResponse, OperationType operationType) {
        Log.i(TAG, "onSuccessFromRemote" + operationType);

        switch (operationType) {
            case FETCH_WEEKLY_RACES:
                handleWeeklyRacesSuccess(weeklyRaceAPIResponse);
                break;
            case FETCH_NEXT_RACE:
                handleNextRaceSuccess(weeklyRaceAPIResponse);
                break;
            case FETCH_LAST_RACE:
                handleLastRaceSuccess(weeklyRaceAPIResponse);
                break;
            default:
                Log.i(TAG, "Operation type not supported");
        }
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

    private void handleWeeklyRacesSuccess(RaceAPIResponse weeklyRaceAPIResponse) {
        List<RaceDTO> raceDTOS = weeklyRaceAPIResponse.getRaceTable().getRaces();
        List<WeeklyRace> weeklyRaceList = new ArrayList<>();
        for (RaceDTO raceDTO : raceDTOS) {
            weeklyRaceList.add(WeeklyRaceMapper.toWeeklyRace(raceDTO));
        }
        Result.WeeklyRaceSuccess result = new Result.WeeklyRaceSuccess(weeklyRaceList);
        weeklyRaceMutableLiveData.postValue(result);
        //Collections.reverse(weeklyRaceList);
        //raceLocalDataSource.insertWeeklyRaceList(weeklyRaceList);
    }

    void handleNextRaceSuccess(RaceAPIResponse weeklyRaceAPIResponse) {
        Log.i(TAG, "handleNextRaceSuccess");
        WeeklyRace race = WeeklyRaceMapper.toWeeklyRace(weeklyRaceAPIResponse.getRaceTable().getRaces().get(0));
        Log.i(TAG, "handleNextRaceSuccess" + race);
        nextRaceMutableLiveData.postValue(new Result.NextRaceSuccess(race));
    }

    void handleLastRaceSuccess(RaceAPIResponse weeklyRaceAPIResponse) {
        if (weeklyRaceAPIResponse.getRaceTable().getRaces().isEmpty() || weeklyRaceAPIResponse.getRaceTable().getRace().getRound().equals("1")) {
            lastRaceMutableLiveData.postValue(new Result.Error("no races available"));
        } else {
            WeeklyRace weeklyRace = WeeklyRaceMapper.toWeeklyRaceNext(weeklyRaceAPIResponse.getRaceTable().getRaces().get(0));
            lastRaceMutableLiveData.postValue(new Result.NextRaceSuccess(weeklyRace));
        }

    }
}
