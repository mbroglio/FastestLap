package com.the_coffe_coders.fastestlap.repository.result;

import android.os.Handler;
import android.os.Looper;
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
import java.util.List;

public class RaceResultRepository implements IRaceResultRepository, RaceResultResponseCallback {

    private static final String TAG = "RaceResultRepository";
    public static boolean isOutdate = true;
    private final MutableLiveData<Result> raceResultMutableLiveData;
    private final MutableLiveData<Result> allRaceResultMutableLiveData;
    private final BaseRaceResultRemoteDataSource raceResultRemoteDataSource;
    private final List<Race> raceList = new ArrayList<>();
    private final BaseRaceResultLocalDataSource raceResultLocalDataSource;


    public RaceResultRepository(BaseRaceResultRemoteDataSource raceResultRemoteDataSource, BaseRaceResultLocalDataSource raceResultLocalDataSource) {
        this.raceResultMutableLiveData = new MutableLiveData<>();
        this.allRaceResultMutableLiveData = new MutableLiveData<>();
        this.raceResultRemoteDataSource = raceResultRemoteDataSource;
        this.raceResultLocalDataSource = raceResultLocalDataSource;
        this.raceResultRemoteDataSource.setRaceResultCallback(this);
        this.raceResultLocalDataSource.setRaceResultCallback(this);
    }

    private synchronized void addRaces(Race race){
        raceList.add(race);
    }

    @Override
    public MutableLiveData<Result> fetchRaceResults(int round, long lastUpdate) {
        Log.i(TAG, "fetchRaceResults");
        if (true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT)
            //TODO fetch from remote
            Log.i(TAG, "Remote fetchRaceResults");
            raceResultRemoteDataSource.getRaceResults(round);
            isOutdate = false;
        } else {
            Log.i(TAG, "fetchRaceResults from local");
            raceResultLocalDataSource.getRaceResults();
        }
        return raceResultMutableLiveData;
    }

    public MutableLiveData<Result> fetchAllRaceResults(long lastUpdate){
        Log.i(TAG, "fetchAllRaceResults");
        if (true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT)
            //TODO fetch from remote
            Log.i(TAG, "Remote fetchAllRaceResults");
            for (int i = 1; i <= 24; i++) {
                raceResultRemoteDataSource.getRaceResults(i);
            }
            isOutdate = false;
        }else{
            Log.i(TAG, "fetchAllRaceResults from local");
            raceResultLocalDataSource.getAllRaceResult();
        }
     return allRaceResultMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> fetchRaceResult(String resultId, long lastUpdate) {
        return null;
    }


    @Override
    public void onSuccessFromRemote(RaceResultsAPIResponse raceResultsAPIResponse) {
        Log.i(TAG, "onSuccessFromRemote");
        Log.i(TAG, raceResultsAPIResponse.toString());
        addRaces(RaceMapper.toRace(raceResultsAPIResponse.getFinalRace()));
        List<RaceResult> raceResult = new ArrayList<>();
        for (ResultDTO resultDTO : raceResultsAPIResponse.getRaceResults()) {
            raceResult.add(SessionMapper.toResult(resultDTO));
        }
        if(raceList.size() == 24){
            allRaceResultMutableLiveData.postValue(new Result.RaceSuccess(raceList));
        }
        raceResultLocalDataSource.insertRaceResultList(raceResult);

    }

    @Override
    public void onFailureFromRemote(Exception exception) {

    }

    @Override
    public void onSuccessFromLocal(RaceResult raceResult) {

    }

    @Override
    public void onSuccessFromLocal(List<RaceResult> raceResultList) {
        Log.i(TAG, "onSuccessFromLocal");
        new Handler(Looper.getMainLooper()).post(() -> {
            raceResultMutableLiveData.setValue(new Result.RaceResultSuccess(raceResultList));
        });
        Log.i(TAG, raceResultMutableLiveData.toString());
    }

    @Override
    public void onFailureFromLocal(Exception exception) {

    }
}
