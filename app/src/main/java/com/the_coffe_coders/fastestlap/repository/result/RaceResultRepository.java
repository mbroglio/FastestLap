package com.the_coffe_coders.fastestlap.repository.result;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.RaceResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.dto.ResultDTO;
import com.the_coffe_coders.fastestlap.mapper.RaceMapper;
import com.the_coffe_coders.fastestlap.mapper.SessionMapper;
import com.the_coffe_coders.fastestlap.source.result.BaseRaceResultLocalDataSource;
import com.the_coffe_coders.fastestlap.source.result.BaseRaceResultRemoteDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaceResultRepository implements RaceResultResponseCallback {

    private static final String TAG = "RaceResultRepository";
    public static Map<Integer, Boolean> isOutdateRaceResult = new HashMap<>();

    //Make a Map of race results for each round
    public static boolean isOutdateRaceResults = true;
    private final MutableLiveData<Result> allRaceResultMutableLiveData;
    private final BaseRaceResultRemoteDataSource raceResultRemoteDataSource;
    private final BaseRaceResultLocalDataSource raceResultLocalDataSource;
    private List<Race> raceList;

    private int numberOfRaces;


    public RaceResultRepository(BaseRaceResultRemoteDataSource raceResultRemoteDataSource, BaseRaceResultLocalDataSource raceResultLocalDataSource) {
        this.allRaceResultMutableLiveData = new MutableLiveData<>();
        this.raceResultRemoteDataSource = raceResultRemoteDataSource;
        this.raceResultLocalDataSource = raceResultLocalDataSource;
        this.raceResultRemoteDataSource.setRaceResultCallback(this);
        this.raceResultLocalDataSource.setRaceResultCallback(this);
    }

    private synchronized void addRaces(Race race) {
        raceList.add(race);
        if (raceList.size() == numberOfRaces) {
            allRaceResultMutableLiveData.setValue(new Result.RaceSuccess(raceList));
            Log.i(TAG, "posting value!!! + " + raceList.size());
            //raceResultLocalDataSource.insertRaceList(raceList);
        }
        //Log.i(TAG, race.toString());
    }

    public MutableLiveData<Result> fetchAllRaceResults(long lastUpdate, int numberOfRaces) {
        Log.i(TAG, "fetchAllRaceResults");
        raceList = new ArrayList<>();
        this.numberOfRaces = numberOfRaces;
        raceResultRemoteDataSource.getAllRaceResults(numberOfRaces);
        isOutdateRaceResults = false;
        return allRaceResultMutableLiveData;
    }

    @Override
    public void onSuccessFromRemote(RaceResultsAPIResponse raceResultsAPIResponse) {
        if (raceResultsAPIResponse.getFinalRace() == null) {
            allRaceResultMutableLiveData.postValue(new Result.Error("No data available"));
            return;
        }
        addRaces(RaceMapper.toRace(raceResultsAPIResponse.getFinalRace()));
    }

    @Override
    public void onFailureFromRemote(Exception exception) {

    }

    @Override
    public void onSuccessFromLocal(RaceResult raceResult) {

    }

    @Override
    public void onSuccessFromLocal(List<RaceResult> raceResultList) {

    }

    @Override
    public void onSuccessFromLocalRaceList(List<Race> raceList) {
        Log.i(TAG, "onSuccessFromLocalRaceList" + raceList.size());
        allRaceResultMutableLiveData.setValue(new Result.RaceSuccess(raceList));
    }


    @Override
    public void onFailureFromLocal(Exception exception) {

    }
}
