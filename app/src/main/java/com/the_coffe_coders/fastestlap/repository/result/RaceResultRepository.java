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

public class RaceResultRepository implements IRaceResultRepository, RaceResultResponseCallback {

    private static final String TAG = "RaceResultRepository";

    public static Map<Integer, Boolean> isOutdateRaceResult = new HashMap<>();
    public static boolean isOutdateRaceResults = true;
    private final MutableLiveData<Result> raceResultMutableLiveData;
    private final MutableLiveData<Result> allRaceResultMutableLiveData;
    private final MutableLiveData<Result> lastRaceResultsMutableLiveData;
    private final MutableLiveData<Result> singleRaceMutableLiveData;
    private final BaseRaceResultRemoteDataSource raceResultRemoteDataSource;
    private final BaseRaceResultLocalDataSource raceResultLocalDataSource;
    private List<Race> raceList;

    private int numberOfRaces;


    public RaceResultRepository(BaseRaceResultRemoteDataSource raceResultRemoteDataSource, BaseRaceResultLocalDataSource raceResultLocalDataSource) {
        this.raceResultMutableLiveData = new MutableLiveData<>();
        this.allRaceResultMutableLiveData = new MutableLiveData<>();
        this.lastRaceResultsMutableLiveData = new MutableLiveData<>();
        this.raceResultRemoteDataSource = raceResultRemoteDataSource;
        this.raceResultLocalDataSource = raceResultLocalDataSource;
        this.raceResultRemoteDataSource.setRaceResultCallback(this);
        this.raceResultLocalDataSource.setRaceResultCallback(this);
        singleRaceMutableLiveData = new MutableLiveData<>();
    }

    private synchronized void addRaces(Race race) {
        raceList.add(race);
        if (raceList.size() == numberOfRaces) {
            allRaceResultMutableLiveData.postValue(new Result.RaceSuccess(raceList));
            Log.i(TAG, "posting value!!! + " + raceList.size());
            raceResultLocalDataSource.insertRaceList(raceList);
            raceResultMutableLiveData.postValue(new Result.RaceSuccess(raceList));
        }
        //Log.i(TAG, race.toString());
    }


    public MutableLiveData<Result> fetchLastRaceResults(long lastUpdate) {
        Log.i(TAG, "fetchLastRaceResults");
        raceResultRemoteDataSource.getLastRaceResults();
        return lastRaceResultsMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> fetchRaceResult(int round, long lastUpdate) {
        Log.i(TAG, "fetchRaceResults");
        if (isOutdateRaceResult.get(round) == null) {
            isOutdateRaceResult.put(round, true);
        }
        if (isOutdateRaceResult.get(round)) {
            //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT)
            //TODO fetch from remote
            Log.i(TAG, "Remote fetchRaceResults");
            raceResultRemoteDataSource.getRaceResults(round);
            isOutdateRaceResult.put(round, false);

        } else {
            Log.i(TAG, "fetchRaceResults from local");
            raceResultLocalDataSource.getAllRaceResult();
        }

        return raceResultMutableLiveData;
    }

    public MutableLiveData<Result> fetchAllRaceResults(long lastUpdate, int numberOfRaces) {
        Log.i(TAG, "fetchAllRaceResults");
        raceList = new ArrayList<>();
        this.numberOfRaces = numberOfRaces;
        if (true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT)
            //TODO fetch from remote
            //Log.i(TAG, "Remote fetchAllRaceResults");
            raceResultRemoteDataSource.getAllRaceResults(numberOfRaces);

            isOutdateRaceResults = false;
        } else {
            Log.i(TAG, "fetchAllRaceResults from local");
            raceResultLocalDataSource.getAllRaceList();
        }
        Log.i(TAG, "fetchallreturn");
        return allRaceResultMutableLiveData;
    }

    @Override
    public void onSuccessFromRemote(RaceResultsAPIResponse raceResultsAPIResponse) {

    }

    @Override
    public void onSuccessFromRemote(RaceResultsAPIResponse raceResultsAPIResponse, int type) {
        //Log.i(TAG, "onSuccessFromRemote");
        //Log.i(TAG, raceResultsAPIResponse.toString());

        System.out.println(type);
        if (type == 0) {
            Race race = RaceMapper.toRace(raceResultsAPIResponse.getFinalRace());
            lastRaceResultsMutableLiveData.postValue(new Result.LastRaceResultsSuccess(race));
        } else if (type == 1) {
            if (raceResultsAPIResponse.getFinalRace() == null) {
                raceResultMutableLiveData.postValue(new Result.Error("No data available"));
                allRaceResultMutableLiveData.postValue(new Result.Error("No data available"));
                return;
            }
            addRaces(RaceMapper.toRace(raceResultsAPIResponse.getFinalRace()));
        } else {
            List<RaceResult> raceResult = new ArrayList<>();
            for (ResultDTO resultDTO : raceResultsAPIResponse.getRaceResults()) {
                raceResult.add(SessionMapper.toResult(resultDTO));
            }
            raceResultLocalDataSource.insertRaceResultList(raceResult);
            //raceResultMutableLiveData.postValue(new Result.RaceResultSuccess(raceResult));
        }
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
       /* new Handler(Looper.getMainLooper()).post(() -> {
            raceResultMutableLiveData.setValue(new Result.RaceResultSuccess(raceResultList));
        });*/
        allRaceResultMutableLiveData.setValue(new Result.RaceSuccess(raceList));
        Log.i(TAG, allRaceResultMutableLiveData.toString());
    }


    @Override
    public void onFailureFromLocal(Exception exception) {

    }
}
